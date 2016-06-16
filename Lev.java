
/**
 * Write a description of class Lev here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Lev
{
    public static int levin(String a, String b){
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

                //d[i][j] = Math.min(d[i-1][j] + 1,
                //    Math.min(d[i][j-1] + 1, d[i-1][j-1] + substitutionCost));
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

    public static double similarity(String a, String b){
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
}
