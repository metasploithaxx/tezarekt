package stream;


import java.io.*;
import java.util.ArrayList;

class TrieNode implements Serializable
{
    static transient final int ALPHABET_SIZE = 256;
    private TrieNode[] children = new TrieNode[ALPHABET_SIZE];
    private int rank =0;
    private boolean isEndOfWord;

    TrieNode(){
        isEndOfWord = false;
        rank =0;
        for (int i = 0; i < ALPHABET_SIZE; i++)
            children[i] = null;
    }

    public TrieNode getChildren(int idx) {
        return children[idx];
    }

    public boolean isEndOfWord() {
        return isEndOfWord;
    }

    public int getValue() {
        return rank;
    }

    public void setValue(int value) {
        this.rank = value;
    }

    public void setChildren(int idx, TrieNode val) {
        this.children[idx] = val;
    }

    public void setEndOfWord(boolean endOfWord) {
        isEndOfWord = endOfWord;
    }
}
public class TrieAutoComplete {

        private TrieNode root;

        public void insert(String key,int rank)
        {
            int level;
            int length = key.length();
            int index;

            TrieNode pCrawl = root;

            for (level = 0; level < length; level++)
            {

                index = key.charAt(level) ;
                if (pCrawl.getChildren(index) == null) {
                    pCrawl.setChildren(index, new TrieNode());
                    pCrawl.setValue(rank);
                }
                pCrawl.setValue(rank);
                pCrawl = pCrawl.getChildren(index);
            }

            // mark last node as leaf
            pCrawl.setEndOfWord(true);
        }

        private void DFS(TrieNode index){

        }

        public ArrayList<String> search(String key)
        {
            int level;
            int length = key.length();
            int index;
            TrieNode pCrawl = root;

            for (level = 0; level < length; level++)
            {
                index = key.charAt(level) ;

                if (pCrawl.getChildren(index) == null)
                    return null;

                pCrawl = pCrawl.getChildren(index);
            }

            if((pCrawl != null && pCrawl.isEndOfWord()){
                return null;
            }
        }

        // Driver
        public static void main(String args[])
        {
            // Input keys (use only 'a' through 'z' and lower case)
            String keys[] = { "a", "there", "answer", "any",
                    "by", "bye", "their"};

            String output[] = {"Not present in trie", "Present in trie"};

            TrieAutoComplete obj=new TrieAutoComplete();
            obj.root=new TrieNode();

            // Construct trie
            int i;
            for (i = 0; i < keys.length ; i++)
                obj.insert(keys[i],1);

            try(FileOutputStream fos=new FileOutputStream("a.txt")){
                ObjectOutputStream oos=new ObjectOutputStream(fos);
                oos.writeObject(obj.root);
                oos.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            TrieNode newRoot=null;
            try(FileInputStream fos=new FileInputStream("a.txt")){
                ObjectInputStream oos=new ObjectInputStream(fos);
                newRoot=(TrieNode)oos.readObject();
                oos.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            obj.root=newRoot;

            // Search for different keys
            if(obj.search("the") == true)
                System.out.println("the --- " + output[1]);
            else System.out.println("the --- " + output[0]);

            if(obj.search("these") == true)
                System.out.println("these --- " + output[1]);
            else System.out.println("these --- " + output[0]);

            if(obj.search("their") == true)
                System.out.println("their --- " + output[1]);
            else System.out.println("their --- " + output[0]);

            if(obj.search("thaw") == true)
                System.out.println("thaw --- " + output[1]);
            else System.out.println("thaw --- " + output[0]);

        }

// This code is contributed by Sumit Ghosh

}
