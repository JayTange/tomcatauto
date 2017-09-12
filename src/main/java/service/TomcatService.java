package service;

import base.TomcateServerBase;

public class TomcatService extends TomcateServerBase implements IService{

	/**
	 * 启用http的服务端口
	 */
	
	public TomcatService(String baseDir,int httpPort) {
		super(baseDir);
		this.httpPort = httpPort;
	}


	
	@Override
	public void start() throws Exception {
		this.initServer();
		this.setHttpServer(httpPort);
		this.startServer();
	}

	@Override
	public void stop() throws Exception {
		if(tomcat!=null){
			tomcat.stop();
			System.out.println("Tomcat service stop success!");
		}
	}

}
