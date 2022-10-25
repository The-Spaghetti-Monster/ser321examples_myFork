/*
Simple Web Server in Java which allows you to call 
localhost:9000/ and show you the root.html webpage from the www/root.html folder
You can also do some other simple GET requests:
1) /random shows you a random picture (well random from the set defined)
2) json shows you the response as JSON for /random instead the html page
3) /file/filename shows you the raw file (not as HTML)
4) /multiply?num1=3&num2=4 multiplies the two inputs and responses with the result
5) /github?query=users/amehlhase316/repos (or other GitHub repo owners) will lead to receiving
   JSON which will for now only be printed in the console. See the todo below

The reading of the request is done "manually", meaning no library that helps making things a 
little easier is used. This is done so you see exactly how to pars the request and 
write a response back
*/

package funHttpServer;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;
import java.util.LinkedHashMap;
import java.nio.charset.Charset;
import org.json.*;

class WebServer {
  public static void main(String args[]) {
    WebServer server = new WebServer(9000);
  }

  /**
   * Main thread
   * @param port to listen on
   */
  public WebServer(int port) {
    ServerSocket server = null;
    Socket sock = null;
    InputStream in = null;
    OutputStream out = null;

    try {
      server = new ServerSocket(port);
      while (true) {
        sock = server.accept();
        out = sock.getOutputStream();
        in = sock.getInputStream();
        byte[] response = createResponse(in);
        out.write(response);
        out.flush();
        in.close();
        out.close();
        sock.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (sock != null) {
        try {
          server.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Used in the "/random" endpoint
   */
  private final static HashMap<String, String> _images = new HashMap<>() {
    {
      put("streets", "https://iili.io/JV1pSV.jpg");
      put("bread", "https://iili.io/Jj9MWG.jpg");
    }
  };

  private Random random = new Random();

  /**
   * Reads in socket stream and generates a response
   * @param inStream HTTP input stream from socket
   * @return the byte encoded HTTP response
   */
  public byte[] createResponse(InputStream inStream) {

    byte[] response = null;
    BufferedReader in = null;

    try {

      // Read from socket's input stream. Must use an
      // InputStreamReader to bridge from streams to a reader
      in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));

      // Get header and save the request from the GET line:
      // example GET format: GET /index.html HTTP/1.1

      String request = null;

      boolean done = false;
      while (!done) {
        String line = in.readLine();

        System.out.println("Received: " + line);

        // find end of header("\n\n")
        if (line == null || line.equals(""))
          done = true;
        // parse GET format ("GET <path> HTTP/1.1")
        else if (line.startsWith("GET")) {
          int firstSpace = line.indexOf(" ");
          int secondSpace = line.indexOf(" ", firstSpace + 1);

          // extract the request, basically everything after the GET up to HTTP/1.1
          request = line.substring(firstSpace + 2, secondSpace);
        }

      }
      System.out.println("FINISHED PARSING HEADER\n");

      // Generate an appropriate response to the user
      if (request == null) {
        response = "<html>Illegal request: no GET</html>".getBytes();
      } else {
        // create output buffer
        StringBuilder builder = new StringBuilder();
        // NOTE: output from buffer is at the end

        if (request.length() == 0) {
          // shows the default directory page

          // opens the root.html file
          String page = new String(readFileInBytes(new File("www/root.html")));
          // performs a template replacement in the page
          page = page.replace("${links}", buildFileList());

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(page);

        } else if (request.equalsIgnoreCase("json")) {
          // shows the JSON of a random image and sets the header name for that image

          // pick a index from the map
          int index = random.nextInt(_images.size());

          // pull out the information
          String header = (String) _images.keySet().toArray()[index];
          String url = _images.get(header);

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: application/json; charset=utf-8\n");
          builder.append("\n");
          builder.append("{");
          builder.append("\"header\":\"").append(header).append("\",");
          builder.append("\"image\":\"").append(url).append("\"");
          builder.append("}");

        } else if (request.equalsIgnoreCase("random")) {
          // opens the random image page

          // open the index.html
          File file = new File("www/index.html");

          // Generate response
          builder.append("HTTP/1.1 200 OK\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(new String(readFileInBytes(file)));

        } else if (request.contains("file/")) {
          // tries to find the specified file and shows it or shows an error

          // take the path and clean it. try to open the file
          File file = new File(request.replace("file/", ""));

          // Generate response
          if (file.exists()) { // success
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append(new String(readFileInBytes(file)));
          } else { // failure
            builder.append("HTTP/1.1 404 Not Found\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("File not found: " + file);
          }
        } else if (request.contains("multiply?")) {
          // This multiplies two numbers, there is NO error handling, so when
          // wrong data is given this just crashes

          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          // extract path parameters
          try{
          query_pairs = splitQuery(request.replace("multiply?", ""));

          // extract required fields from parameters
          Integer num1 = Integer.parseInt(query_pairs.get("num1"));
          Integer num2 = Integer.parseInt(query_pairs.get("num2"));

          // do math
          Integer result = num1 * num2;


          // Generate response
            builder.append("HTTP/1.1 200 OK\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Result is: " + result);
          }


          // TODO: Include error handling here with a correct error code and
          // a response that makes sense
          catch(Exception e){
            builder.append("HTTP/1.1 400 Bad Request\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append("Must Multiply Two Numbers (i.e./multiply?num1=5&num2=10). Both parameters required.");
          }


        }
        else if (request.contains("github?")) {
          // pulls the query from the request and runs it with GitHub's REST API
          // check out https://docs.github.com/rest/reference/
          //
          // HINT: REST is organized by nesting topics. Figure out the biggest one first,
          //     then drill down to what you care about
          // "Owner's repo is named RepoName. Example: find RepoName's contributors" translates to
          //     "/repos/OWNERNAME/REPONAME/contributors"


          try {
            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            try {
              query_pairs = splitQuery(request.replace("github?", ""));
            }
            catch(StringIndexOutOfBoundsException e){
              e.printStackTrace();
            }
            String json = fetchURL("https://api.github.com/" + query_pairs.get("query"));
            if(query_pairs.get("query") == null){
              builder.append("HTTP/1.1 400 Bad Request, No query\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("No query");
            }
            else if(json.isEmpty()){
              builder.append("HTTP/1.1 400 Bad Request, URL not Fetched\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("URL Not Fetched");
            }
            else {
              System.out.println(json);

              builder.append("HTTP/1.1 200 OK\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              // TODO: Parse the JSON returned by your fetch and create an appropriate
              // response based on what the assignment document asks for
              try {
                JSONArray objArr = new JSONArray(json);
                JSONObject obj;
                JSONObject owners;
                builder.append("[\n");
                for (int i = 0; i < objArr.length(); i++) {
                  obj = new JSONObject(objArr.get(i).toString());
                  owners = new JSONObject(obj.get("owner").toString());
                  builder.append("{" + "\"full_name\": " + obj.get("full_name") + "," + "\n");
                  builder.append("\"id\": " + obj.get("id") + "," + "\n");
                  builder.append("\"loginname\": " + owners.get("login") + "}");
                }
                builder.append("]\n");
              } catch (JSONException e) {
                e.printStackTrace();
                builder.append(e.getMessage());
                builder.append("\n\nThe JSON could not be parsed.");
              }
            }
          }
          catch(StringIndexOutOfBoundsException e){
            e.printStackTrace();
            builder.append(e.getMessage());
          }


        }
        // guessing game
        else if(request.contains("guess?")){
          try {
            int ans = random.nextInt(10);
            Map<String, String> query_pairs = new LinkedHashMap<String, String>();
            query_pairs = splitQuery(request.replace("guess?", ""));

            Integer num1 = Integer.parseInt(query_pairs.get("num1"));
            Integer num2 = Integer.parseInt(query_pairs.get("num2"));

            if(num1 > 10 || num2 > 10 || num1 < 0 || num2 < 0){
              builder.append("HTTP/1.1 400 Bad Request, Guess not in range\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append("num1 and num2 must stay between 0 and 10");
            }
            else {
              String output = "";
              if(num1 != ans && num2 != ans) {
                output = "Wrong,";
                if (num1 > ans) {
                  int dist = num1 - ans;
                  output = output + " num1 is " + dist + " off";
                }
                else{
                  int dist = ans - num1;
                  output = output + " num1 is " + dist + " off";
                }

                if (num2 > ans) {
                  int dist = num2 - ans;
                  output = output + " and num2 is " + dist + " off";
                }
                else{
                  int dist = ans - num2;
                  output = output + " and num2 is " + dist + " off";
                }
              }
              else{
                output = "Correct the answer is: " + ans;
              }
              builder.append("HTTP/1.1 200 OK, Guessing game played\n");
              builder.append("Content-Type: text/html; charset=utf-8\n");
              builder.append("\n");
              builder.append(output);
            }
          }
          catch(StringIndexOutOfBoundsException e){
            builder.append("HTTP/1.1 400 Bad Request, Neither parameter used\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append(" StringIndexOutOfBoundsException. Make sure to use all parameters");
          }
          catch(NumberFormatException e){
            builder.append("HTTP/1.1 400 Bad Request, One parameter missing\n");
            builder.append("Content-Type: text/html; charset=utf-8\n");
            builder.append("\n");
            builder.append(e.getMessage() + " Make sure to use all parameters");
          }

        }
        else if(request.contains("palindrome?")){
          Map<String, String> query_pairs = new LinkedHashMap<String, String>();
          try {
            query_pairs = splitQuery(request.replace("palindrome?", ""));
          }
          catch(StringIndexOutOfBoundsException e) {
            e.printStackTrace();
          }


          String str1 = query_pairs.get("str1");
          String str2 = query_pairs.get("str2");

          String response1 = str1 + " is a palindrome";
          String response2 = str2 + " is a palindrome";

          if(str1 == null){
            response1 = "str1 is null ";
          }
          else{
            for(int i = 0; i<str1.length()/2; i++){
              str1.toLowerCase();
              boolean pal = true;
              if(str1.charAt(i) != str1.charAt((str1.length()-1)-i)){
                pal = false;
                response1 = str1 + " is not a palindrome";
                break;
              }
            }
          }

          if(str2 == null){
            response2 = "str2 is null ";
          }
          else{
            for(int i = 0; i<str2.length()/2; i++){
              str1.toLowerCase();
              boolean pal = true;
              if(str2.charAt(i) != str2.charAt((str2.length()-1)-i)){
                pal = false;
                response2 = str2 + " is not a palindrome";
                break;
              }
            }
          }


          builder.append("HTTP/1.1 200 OK, Palindrome checked\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append(response1 + " and " + response2);

        }
        else {
          // if the request is not recognized at all

          builder.append("HTTP/1.1 400 Bad Request\n");
          builder.append("Content-Type: text/html; charset=utf-8\n");
          builder.append("\n");
          builder.append("I am not sure what you want me to do...");
        }

        // Output
        response = builder.toString().getBytes();
      }
    } catch (IOException e) {
      e.printStackTrace();
      response = ("<html>ERROR: " + e.getMessage() + "</html>").getBytes();
    }

    return response;
  }

  /**
   * Method to read in a query and split it up correctly
   * @param query parameters on path
   * @return Map of all parameters and their specific values
   * @throws UnsupportedEncodingException If the URLs aren't encoded with UTF-8
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    // "q=hello+world%2Fme&bob=5"
    String[] pairs = query.split("&");
    // ["q=hello+world%2Fme", "bob=5"]
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    // {{"q", "hello world/me"}, {"bob","5"}}
    return query_pairs;
  }

  /**
   * Builds an HTML file list from the www directory
   * @return HTML string output of file list
   */
  public static String buildFileList() {
    ArrayList<String> filenames = new ArrayList<>();

    // Creating a File object for directory
    File directoryPath = new File("www/");
    filenames.addAll(Arrays.asList(directoryPath.list()));

    if (filenames.size() > 0) {
      StringBuilder builder = new StringBuilder();
      builder.append("<ul>\n");
      for (var filename : filenames) {
        builder.append("<li>" + filename + "</li>");
      }
      builder.append("</ul>\n");
      return builder.toString();
    } else {
      return "No files in directory";
    }
  }

  /**
   * Read bytes from a file and return them in the byte array. We read in blocks
   * of 512 bytes for efficiency.
   */
  public static byte[] readFileInBytes(File f) throws IOException {

    FileInputStream file = new FileInputStream(f);
    ByteArrayOutputStream data = new ByteArrayOutputStream(file.available());

    byte buffer[] = new byte[512];
    int numRead = file.read(buffer);
    while (numRead > 0) {
      data.write(buffer, 0, numRead);
      numRead = file.read(buffer);
    }
    file.close();

    byte[] result = data.toByteArray();
    data.close();

    return result;
  }

  /**
   *
   * a method to make a web request. Note that this method will block execution
   * for up to 20 seconds while the request is being satisfied. Better to use a
   * non-blocking request.
   * 
   * @param aUrl the String indicating the query url for the OMDb api search
   * @return the String result of the http request.
   *
   **/
  public String fetchURL(String aUrl) {
    StringBuilder sb = new StringBuilder();
    URLConnection conn = null;
    InputStreamReader in = null;
    try {
      URL url = new URL(aUrl);
      conn = url.openConnection();
      if (conn != null)
        conn.setReadTimeout(20 * 1000); // timeout in 20 seconds
      if (conn != null && conn.getInputStream() != null) {
        in = new InputStreamReader(conn.getInputStream(), Charset.defaultCharset());
        BufferedReader br = new BufferedReader(in);
        if (br != null) {
          int ch;
          // read the next character until end of reader
          while ((ch = br.read()) != -1) {
            sb.append((char) ch);
          }
          br.close();
        }
      }
      in.close();
    } catch (Exception ex) {
      System.out.println("Exception in url request:" + ex.getMessage());
    }
    return sb.toString();
  }
}
