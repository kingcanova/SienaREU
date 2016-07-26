
/**
 * This class is used to compare the similarity of two strings using the Levenshtein distance algorithm. The point of the class is so that we can check whether the 
 * information we are retrieving is actually relevant by checking it against our target strings,
 * but allows some slight variation between the returned info and the target so things such as The dairy cow and dairy cow are seen as the same/relevant.
 * 
 * @author Tristan Canova, Dan Carpenter, Neil Devine, Kevin Danaher
 * @version 7/26/16
 */
public class Lev
{
    /**
     * Levenshtein distance (LD) is a measure of the similarity between two strings.
     */
    public static int levin(String a, String b)
    {
        int[][] d = new int[a.length()+1][b.length()+1];
        for(int i = 1; i <= a.length(); i++){
            d[i][0] = i;
        }

        for(int j = 1; j <= b.length(); j++){
            d[0][j] = j;
        }

        for(int j = 1; j <= b.length(); j++){
            for(int i = 1; i <= a.length(); i++){
                int substitutionCost;
                if(Character.toLowerCase(a.charAt(i-1)) == Character.toLowerCase(b.charAt(j-1))){
                    substitutionCost = 0;
                }
                else{
                    substitutionCost = 2;
                }
                int cI = d[i-1][j]+1; // Insertion
                int cD = d[i][j-1]+1; // Deletion
                int cS = d[i-1][j-1] + substitutionCost; // Substitution
                if(cI <= cD){
                    if(cI <= cS){
                        d[i][j] = cI;
                    }else{
                        d[i][j] = cS;
                    }
                }else{
                    if(cD <= cS){
                        d[i][j] = cD;
                    }else{
                        d[i][j] = cS;
                    }
                }

            }
        }
        return d[a.length()][b.length()];
    }

    public static double valueWords(String s1, String s2){
        String[] wordsS1 = s1.split(" ");
        String[] wordsS2 = s2.split(" ");
        double wordsTotal = 0.0;
        for(String word : wordsS1){
            int wordBest = s2.length();
            innerloop:
            for(String word2 : wordsS2){
                int thisD = levin(word, word2);
                if(thisD < wordBest){
                    wordBest = thisD;
                }
                if(thisD == 0){
                    break innerloop;
                }
            }
            wordsTotal += wordBest;
        }
        return wordsTotal;
    }

    /**
     * This is the method called to complete the function of the class. 
     * For more information check this link: http://stackoverflow.com/questions/5859561/getting-the-closest-string-match?noredirect=1&lq=1
     */
    public static double similarity(String a, String b)
    {
        int phraseVal = levin(a, b);
        double wordsVal = valueWords(a, b);
        double phraseWeight = 0.5;
        double wordsWeight = 1.0;
        double lengthWeight = -0.3;
        double minWeight = 10;
        double maxWeight = 1;
        double lengthVal = Math.abs(a.length() - b.length());
        lengthVal = 0;
        return Math.min(phraseWeight*phraseVal, wordsWeight*wordsVal)*minWeight
        + Math.max(phraseWeight*phraseVal, wordsWeight*wordsVal)*maxWeight
        + lengthWeight*lengthVal;
    }
    
    public static double categorySimilarity(String a, String b)
    {
        int phraseVal = levin(a, b);
        double wordsVal = valueWords(a, b);
        double phraseWeight = 0.7;
        double wordsWeight = 1.0;
        double lengthWeight = 0.3;
        double minWeight = 10;
        double maxWeight = 1;
        double lengthVal = Math.abs(a.length() - b.length());
        return Math.min(phraseWeight*phraseVal, wordsWeight*wordsVal)*minWeight
        + Math.max(phraseWeight*phraseVal, wordsWeight*wordsVal)*maxWeight
        + lengthWeight*lengthVal;
    }
}
