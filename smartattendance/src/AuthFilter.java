import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                          ServletResponse response,
                          FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();

        String path = uri.substring(contextPath.length());

        // Public pages
        boolean publicPage =
                path.equals("/") ||
                path.equals("/index.html") ||
                path.equals("/home.html") ||
                path.equals("/register.html") ||
                path.equals("/login.html") ||
                path.equals("/RegisterServlet") ||
                path.equals("/LoginServlet") ||
                path.equals("/style.css") ||
                path.equals("/script.js") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".jpeg") ||
                path.endsWith(".gif") ||
                path.endsWith(".svg") ||
                path.endsWith(".ico");

        if (publicPage) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);

        boolean loggedIn =
                session != null &&
                session.getAttribute("userId") != null &&
                Boolean.TRUE.equals(session.getAttribute("passwordVerified"));

        if (loggedIn) {
            chain.doFilter(request, response);
        } else {
            res.sendRedirect(contextPath + "/login.html");
        }
    }
}