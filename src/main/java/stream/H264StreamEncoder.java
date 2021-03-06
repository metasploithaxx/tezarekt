package stream;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.IPixelFormat.Type;
import com.xuggle.xuggler.IStreamCoder.Direction;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.sound.sampled.AudioFormat;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class H264StreamEncoder{
	protected final static Logger logger = LoggerFactory.getLogger(Logger.class);
	protected final IStreamCoder iStreamCoder = IStreamCoder.make(Direction.ENCODING, ICodec.ID.CODEC_ID_H264);
	protected final IStreamCoder iAudioStreamCoder = IStreamCoder.make(Direction.ENCODING, ICodec.ID.CODEC_ID_AAC);
	protected final IPacket iPacket = IPacket.make();
	protected long startTime ;
	protected final Dimension dimension;

	protected final AudioFormat format = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED,
			44100.0F, 16, 2, 4, 44100, false);


	public H264StreamEncoder(Dimension dimension) {
		super();
		this.dimension = dimension;
		initialize();
	}

	private void initialize(){
		//setup
		iStreamCoder.setNumPicturesInGroupOfPictures(60);

		iStreamCoder.setBitRate(200000);
		iStreamCoder.setBitRateTolerance(10000);
		iStreamCoder.setPixelType(Type.YUV420P);
		iStreamCoder.setHeight(dimension.height);
		iStreamCoder.setWidth(dimension.width);
		iStreamCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
		iStreamCoder.setGlobalQuality(0);
		//rate
		IRational rate = IRational.make(60, 1);
		iStreamCoder.setFrameRate(rate);
		//time base
		iStreamCoder.setTimeBase(IRational.make(rate.getDenominator(),rate.getNumerator()));
		IMetaData codecOptions = IMetaData.make();
		codecOptions.setValue("tune", "zerolatency");// equals -tune zerolatency in ffmpeg
		//open it
		int revl = iStreamCoder.open(codecOptions, null);
		if (revl < 0) {
			throw new RuntimeException("could not open the coder");
		}

		iStreamCoder.setNumPicturesInGroupOfPictures(60);


		iAudioStreamCoder.setChannels(2);
		iAudioStreamCoder.setSampleRate(44100);
		IRational ratea = IRational.make(44100, 1);
		IMetaData codecOptionsa = IMetaData.make();
		revl = iAudioStreamCoder.open(codecOptionsa, null);
		if (revl < 0) {
			throw new RuntimeException("could not open the audio coder");
		}
	}


	public ByteBuffer encode(Object msg) throws Exception {
		if (msg == null) {
			return null;
		}
		if (!(msg instanceof BufferedImage)) {
			throw new IllegalArgumentException("your need to pass into an bufferedimage");
		}
		logger.info("encode the frame");
		BufferedImage bufferedImage = (BufferedImage)msg;
		//here is the encode
		//convert the image
		BufferedImage convetedImage = ImageUtils.convertToType(bufferedImage, BufferedImage.TYPE_3BYTE_BGR);
		IConverter converter = ConverterFactory.createConverter(convetedImage, Type.YUV420P);
		//to frame
		long now = System.currentTimeMillis();
		if (startTime == 0) {
			startTime = now;
		}
		IVideoPicture pFrame = converter.toPicture(convetedImage, (now - startTime)*1000);
		iStreamCoder.encodeVideo(iPacket, pFrame, 0) ;
		//free the MEM
		pFrame.delete();
		converter.delete();
		//write to the container
		iPacket.setStreamIndex(0);
		if (iPacket.isComplete()) {
			iPacket.setFlags(1);
			//here we send the package to the remote peer
			try{
				ByteBuffer byteBuffer = iPacket.getByteBuffer();
				System.out.println(byteBuffer.remaining()+"V");
				if (iPacket.isKeyPacket()) {
					logger.info("key frame");
				}
				byteBuffer.rewind();
				return byteBuffer;

			}finally{
				iPacket.reset();
			}
		}else{
			return null;
		}
	}

	public ByteBuffer encode( byte[] data, int numBytesRead) throws Exception {
		IBuffer iBuf = IBuffer.make(null, data, 0, numBytesRead);

		IAudioSamples smp = IAudioSamples.make(iBuf,2,IAudioSamples.Format.FMT_S16);
		smp.setComplete(true, numBytesRead/4, 44100, 2, IAudioSamples.Format.FMT_S16, 0);
		iAudioStreamCoder.encodeAudio(iPacket, smp, 0);

		//write to the container
		if (iPacket.isComplete()) {
			//here we send the package to the remote peer
			try{
				ByteBuffer byteBuffer = iPacket.getByteBuffer();
				System.out.println(byteBuffer.remaining()+"A");
				if (iPacket.isKeyPacket()) {
					logger.info("key frame");
				}
				byteBuffer.rewind();
				return byteBuffer;

			}
			finally {
				iPacket.reset();
			}
		}
		else {
			return encode(data,numBytesRead);
		}
	}

}