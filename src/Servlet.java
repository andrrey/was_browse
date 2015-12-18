import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

public class Servlet extends javax.servlet.http.HttpServlet {
    private final static int LINES_STEP = 5000;
    private int startLine, endLine;
    private String path;
    private boolean table_mode;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss.SSS");

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        out.print("<!DOCTYPE html>\n");
        out.print("<html>\n" +
                "<head>\n" +
                "<title>WAS Browser</title>\n" +
                "</head>\n" +
                "<body>");

        parseParameters(request);
        File pth = new File(path);
        if(pth.isDirectory()) processDirectory(request, out, pth);
        else {
            if(table_mode) processTableFile(request, out, pth);
            else processFile(request, out, pth);
        }

        out.print("</body>\n" + "</html>");

        out.close();
    }

    private void processTableFile(HttpServletRequest request, PrintWriter out, File pth) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;

        out.print("<table border='1'>");

        while((line = br.readLine()) != null){
            out.print("<tr>");
            String parts[] = line.split(" ");

            for (String part : parts) {
                out.print("<td>");

                if(part.matches("^/.+/profiles/.+/logs/ffdc/.+\\.txt$")){ //detect link to log file in WAS ffdc logs
                    out.print("<a href='");
                    out.print(request.getRequestURI() + "?path=" + part);
                    out.print("'>" + part + "</a>");
                }

                else out.print(part);

                out.print("</td>");
            }

            out.print("</tr>");
        }

        out.print("</table>");
    }

    private void processFile(HttpServletRequest request, PrintWriter out, File pth) throws IOException {
        out.print("<table border='1'>");
        out.print("<tr><td>");
        int nextStartLine = startLine - LINES_STEP;
        if (nextStartLine<0) nextStartLine=0;
        int nextEndLine = endLine - LINES_STEP;
        if (nextEndLine<=0) endLine = LINES_STEP;
        out.print("<a href='" + request.getRequestURL() + "?path=" + path + "&start_line=" + nextStartLine +
                "&end_line=" + nextEndLine + "'>");
        out.print("&laquo;</a>");
        out.print("</td><td>");
        nextStartLine = startLine + LINES_STEP;
        nextEndLine = endLine + LINES_STEP;
        out.print("<a href='" + request.getRequestURL() + "?path=" + path + "&start_line=" + nextStartLine +
                "&end_line=" + nextEndLine + "'>");
        out.print("&raquo;</a>");
        out.print("</td></table>");

        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;

        for (int c=0; ; c++) {
            line = br.readLine();
            if(line == null) break;
            if(c>=endLine) break;
            if(c>=startLine) out.print(line + "<br />");
        }

    }

    private void processDirectory(HttpServletRequest request, PrintWriter out, File pth) throws IOException {
        out.print("Absolute path: " + pth.getAbsolutePath() + "<br />");
        out.print("Canonical path: " + pth.getCanonicalPath() + "<br />");
        File[] filesList = pth.listFiles();

        if (filesList != null && filesList.length > 0) {
            out.print("<table border='1'>");
            out.print("<tr><th>Name</th><th>Type</th><th>R</th><th>W</th><th>X</th><th>Hidden</th>" +
                    "<th>Modified</th><th>Length</th></tr>");
            for (File file : filesList) {
                out.print("<tr><td>");
                out.print("<a href='" + request.getRequestURL() + "?path=" + path + "/" + file.getName());
                if(file.getName().matches(".+_exception\\.log")) out.print("&mode=table");
                out.print("'>");
                out.print(file.getName());
                out.print("</a>");
                out.print("</td><td>");

                if (file.isFile()) {
                    out.print("file");
                }
                if (file.isDirectory()) out.print("directory");

                out.print("<td>");
                if(file.canRead()) out.print("X");
                out.print("</td><td>");
                if(file.canWrite()) out.print("X");
                out.print("</td><td>");
                if(file.canExecute()) out.print("X");
                out.print("</td><td>");
                if(file.isHidden()) out.print("Yes");
                else out.print("No");
                out.print("</td><td>");
                out.print(dateFormat.format(new Date(file.lastModified())));
                out.print("</td><td>");
                out.print(file.length());

                out.print("</td></tr>");
            }

            out.print("</table>");
        } else {
            out.print("EMPTY");
        }
    }

    private void parseParameters(HttpServletRequest request) throws IOException {
        path = request.getParameter("path");
        String sStartLine = request.getParameter("start_line");
        String sEndLine = request.getParameter("end_line");
        String mode = request.getParameter("mode");

        table_mode = mode != null && mode.equalsIgnoreCase("table");

        try{
            startLine = Integer.parseInt(sStartLine);
        } catch (Exception e){
            startLine = 0;
        }

        try{
            endLine = Integer.parseInt(sEndLine);
        } catch (Exception e){
            endLine = LINES_STEP;
        }

        if(endLine < startLine) endLine = startLine + LINES_STEP;

        if(null==path) {
            File dot = new File(".");
            path = dot.getCanonicalPath();
        }

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
