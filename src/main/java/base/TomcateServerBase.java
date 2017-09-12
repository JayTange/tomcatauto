package base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.WebXml;
import org.apache.catalina.session.FileStore;
import org.apache.catalina.session.PersistentManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.WebRuleSet;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.descriptor.DigesterFactory;
import org.apache.tomcat.util.digester.Digester;

public class TomcateServerBase {

	/**
	 * TOMCAT 嵌入式引擎
	 */
	protected Tomcat tomcat = null;

	/**
	 * Tomcat Server Base Directory
	 */
	protected String baseDir = null;

	/**
	 * Http端口
	 */
	protected int httpPort = 8080;

	/**
	 * 会话过期时间
	 */
	protected int sessionTimeOut = -1;

	/**
	 * 共享会话
	 */
	protected boolean shareSession = false;

	/**
	 * http绑定IP，默认为所有IP
	 */
	protected String httpBindIP = "0.0.0.0";

	/**
	 * 设置tomcat POST最大提交数据长度，负数表示不限制，注意在Tomcat7.0.65里面不支持设置0
	 */
	protected int maxPostSize = -1;

	protected String uriEncoding = "UTF-8";

	/**
	 * 设置Tomcat的属性，一般适用于HTTP、HTTPS共享的属性，参看：<link> <a href=
	 * "https://tomcat.apache.org/tomcat-7.0-doc/config/http.html" >https://
	 * tomcat.apache.org/tomcat-7.0-doc/config/http.html</a>
	 * 
	 * </link>
	 */
	protected Map<String, Object> tomcatAttributes = new HashMap<String, Object>();

	/**
	 * 
	 * @param baseDir
	 *            应用根路径
	 * @param sessionTimeout
	 *            session过期时间
	 * @param shareSession
	 *            是否共享回话
	 * @param bindIP
	 *            ip地址
	 */
	public TomcateServerBase(String baseDir, int sessionTimeout, boolean shareSession, String bindIP) {
		this.baseDir = baseDir;
		this.sessionTimeOut = sessionTimeout;
		this.shareSession = shareSession;
		this.httpBindIP = bindIP;
	}

	/**
	 * 应用根路径
	 * 
	 * @param baseDir
	 */
	public TomcateServerBase(String baseDir) {
		this(baseDir, -1, false, "0.0.0.0");
	}

	public void setSessionTimeOut(int sessionTimeOut) {
		this.sessionTimeOut = sessionTimeOut;
	}

	protected Map<String, String> parseMimeMappingFromWebXml(String webXmlFilePath) {

		Map<String, String> mimeMappings = new HashMap<String, String>();

		File webXmlFile = new File(webXmlFilePath);
		// 如果不存在直接返回
		if (!webXmlFile.exists()) {
			return mimeMappings;
		}

		FileInputStream is = null;
		try {
			WebXml webXml = new WebXml();
			WebRuleSet webRuleSet = new WebRuleSet(false);
			Digester webDigester;
			webDigester = DigesterFactory.newDigester(false, false, webRuleSet, false);
			webDigester.getParser();
			webDigester.push(webXml);
			is = new FileInputStream(webXmlFile);
			webDigester.parse(is);

			if (webXml.getMimeMappings() != null && webXml.getMimeMappings().size() > 0) {
				mimeMappings = webXml.getMimeMappings();
			}

		} catch (Exception e) {
			System.out.println("Failed to parse the '" + webXmlFilePath + "' file" + e.getMessage());
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return mimeMappings;
	}

	protected void initServer() throws Exception {

		tomcat = new Tomcat();

		try {
			// 2. 启动 Tomcat
			// 2.1. Embedded
			// 获取应用的根路径，如果classes不放在WEB-INF/class下，或对应Jar不放在WEB-INF/lib目录下，此处必须要修改
			// the host name is used to distinguish different temporary work
			// path,
			// eg. work/tomcat/localhost/xxxxxx/、work/tomcat/online/yyyyyy
			tomcat.setHostname("localhost");

			// 设置工作目录,其实没什么用,tomcat需要使用这个目录进行写一些东西
			// Tomcat needs a directory for temp files.
			tomcat.setBaseDir(baseDir);

			// 查找webapp目录
			String appBase = baseDir + File.separator + "WebContent";
			File f = new File(appBase + "/WEB-INF/web.xml");
			if (!f.exists()) {
				appBase = baseDir + File.separator + "webapp";
				f = new File(appBase + "/WEB-INF/web.xml");
				if (!f.exists()) {
					throw new Exception("无法找到页面目录" + appBase);
				}
			}
			System.out.println("Finded Base Web Root is :" + appBase);

			// 如果系统上使用APR，即The Apache Portable Runtime，才需要注册AprLifecycleListener
			// StandardServer server = (StandardServer) tomcat.getServer();
			// Add AprLifecycleListener
			// AprLifecycleListener listener = new AprLifecycleListener();
			// server.addLifecycleListener(listener);
			// 注册关闭端口以进行关闭
			// 由于我们自己会起一个端口进行管理，所以这里不注册8005
			// tomcat.getServer().setPort(8005);

			// 为了自定义MIMETYPE以支持不同类型文件的下载，传统上均在全局默认web.xml中进行定义
			// 由于Tomcat 7在使用嵌入式方式，即使用Tomcat类时，默认强制不使用全局web.xml
			// 使用自己初始化ContextConfig并使用addLifecycleListener加载它的方式会导致启动出错
			// 重载Tomcat构造类中的noDefaultWebXmlPath的结果也是一样
			// 最终发现最直接的方法是调用StandardContext的addMimeMapping方法，增加属性
			// 为了将增加变得直观和容易理解，在本类中使用WebXml类解析全局web.xml
			// 将getMimeMappings()得到的类型，全部加入到StandardContext中
			// 因此在程序中使用koalserverbase组件时，建议在根路径的conf目录中加入一个web.xml
			// 当然，本类会对该文件是否存在进行判断，如果该文件不存在，则不会进行解析
			/** 这里的全局web.xml 我就是用的 上面的文件 */
			File globalWebXml = new File(baseDir+"/conf/web.xml");
			Map<String, String> mimeMappings = parseMimeMappingFromWebXml(globalWebXml.getCanonicalPath());

			// 2.4 context
			Context context = tomcat.addWebapp("/", new File(baseDir).getAbsolutePath());
			StandardContext standardContext = (StandardContext) context;

			// 2.4.1 解析mimeMappings
			if (mimeMappings != null) {
				for (String key : mimeMappings.keySet()) {
					standardContext.addMimeMapping(key, mimeMappings.get(key));
				}
			}

			// 2.4.2 设置超时
			if (sessionTimeOut > 0) {
				standardContext.setSessionTimeout(sessionTimeOut);
			}
			// 2.4.3 设置是否分享session
			standardContext.setCrossContext(shareSession);

			// 2.4.3 解决未实现序列化而报的异常java.io.WriteAbortedException
			PersistentManager persistentManager = new PersistentManager();
			persistentManager.setSaveOnRestart(false);
			persistentManager.setStore(new FileStore());
			standardContext.setManager(persistentManager);

		} catch (Exception e) {
			System.err.println("Http Admin Service failed to start:" + e.getMessage());
			tomcat = null;
		}

	}

	/**
	 * 设置Tomcat属性
	 * 
	 * @param httpPort
	 *            http端口
	 * @throws Exception
	 */
	protected void setHttpServer(int httpPort) throws Exception {
		if (tomcat == null) {
			throw new Exception("tomcat未初始化，无法启动http 服务");
		}
		// 2.5. Connector - HTTP
		// 有一个默认的，不需要自己定义
		Connector httpConnector = tomcat.getConnector();
		httpConnector.setPort(httpPort);
		httpConnector.setMaxPostSize(maxPostSize);
		httpConnector.setURIEncoding(uriEncoding);
		for (String attKey : tomcatAttributes.keySet()) {
			httpConnector.setAttribute(attKey, tomcatAttributes.get(attKey));
		}
		// 绑定IP地址
		IntrospectionUtils.setProperty(httpConnector, "address", httpBindIP);
		this.httpPort = httpPort;
	}

	/**
	 * 启动服务
	 * 
	 * @throws Exception
	 */
	public void startServer() throws Exception {
		if (tomcat == null) {
			throw new Exception("Tomcat尚未初始化，无法启动服务！");
		}

		// tomcat 启动
		tomcat.start();
		if (this.isConnectorListening(httpPort)) {
			System.out.println("HTTP Service is start successfully: {" + httpBindIP + ":" + httpPort + "}");
		}
		tomcat.getServer().await();
	}

	/**
	 * 停止服务
	 * 
	 * @throws Exception
	 */
	public void stopServer() throws Exception {
		if (tomcat != null) {
			tomcat.stop();
			System.out.println("Tomcat service stop success!");
		}
	}

	/**
	 * check the port whether is listening or not
	 * 
	 * @param port
	 *            the listen port
	 * @return true the port is started false the port is stopped
	 */
	public boolean isConnectorListening(int port) {
		if (tomcat != null) {
			Connector[] connectors = tomcat.getService().findConnectors();
			if (connectors != null) {
				for (Connector cct : connectors) {
					if (cct != null && cct.getPort() == port) {
						LifecycleState state = cct.getState();
						return state == LifecycleState.STARTED;
					}
				}
			}
		}
		return false;
	}

}
