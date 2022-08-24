package publisher;

/**Authors : Sheng-Hao Wu, Kevin Li */

/**
 * Content -- in our PubSub system, we design that the content to be separated as text, like a twit or instagram
 * post that has only text. Keywords is to notate this text information, and also allowed subscriber to subscriber that
 * based on these keywords.
 *
 */
public class Content {
    /** 
     * text saved in content file 
    */
    private String text;
    /** 
     * keywords connected to content file 
    */
    private String[] keyWords;

    /** 
     * constructor to initialize a content file
     * 
     * @param txt text to be saved to content file
     * @param kws keywords to be assicated with content file
    */
    public Content(String txt, String[] kws) {
        this.text = txt;
        this.keyWords = kws;
    }

    /** 
     * Function to get text saved in content file
     * 
     * @return text of content file
    */
    public String getText() {
        return this.text;
    }

    /** 
     * Function to get keywords associated with content file
     * 
     * @return keywords associated with content file
    */
    public String[] getKeyWords() {
        return this.keyWords;
    }
}
