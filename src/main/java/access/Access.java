package access;

import com.tomcat.service.TomcatService;

public class Access {

	private static String baseDir= "E:/git/tomcatauto/src/main";
	
	private static int port = 8080;
	
	
	public static void main(String[] args) throws Exception {
		
		TomcatService service = new TomcatService(baseDir, port);
		service.start();
	}

}
