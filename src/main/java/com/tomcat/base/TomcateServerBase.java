package com.tomcat.base;

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
	 * TOMCAT Ƕ��ʽ����
	 */
	protected Tomcat tomcat = null;

	/**
	 * Tomcat Server Base Directory
	 */
	protected String baseDir = null;

	/**
	 * Http�˿�
	 */
	protected int httpPort = 8080;

	/**
	 * �Ự����ʱ��
	 */
	protected int sessionTimeOut = -1;

	/**
	 * ����Ự
	 */
	protected boolean shareSession = false;

	/**
	 * http��IP��Ĭ��Ϊ����IP
	 */
	protected String httpBindIP = "0.0.0.0";

	/**
	 * ����tomcat POST����ύ���ݳ��ȣ�������ʾ�����ƣ�ע����Tomcat7.0.65���治֧������0
	 */
	protected int maxPostSize = -1;

	protected String uriEncoding = "UTF-8";

	/**
	 * ����Tomcat�����ԣ�һ��������HTTP��HTTPS��������ԣ��ο���<link> <a href=
	 * "https://tomcat.apache.org/tomcat-7.0-doc/config/http.html" >https://
	 * tomcat.apache.org/tomcat-7.0-doc/config/http.html</a>
	 * 
	 * </link>
	 */
	protected Map<String, Object> tomcatAttributes = new HashMap<String, Object>();

	/**
	 * 
	 * @param baseDir
	 *            Ӧ�ø�·��
	 * @param sessionTimeout
	 *            session����ʱ��
	 * @param shareSession
	 *            �Ƿ���ػ�
	 * @param bindIP
	 *            ip��ַ
	 */
	public TomcateServerBase(String baseDir, int sessionTimeout, boolean shareSession, String bindIP) {
		this.baseDir = baseDir;
		this.sessionTimeOut = sessionTimeout;
		this.shareSession = shareSession;
		this.httpBindIP = bindIP;
	}

	/**
	 * Ӧ�ø�·��
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
		// ���������ֱ�ӷ���
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
			// 2. ���� Tomcat
			// 2.1. Embedded
			// ��ȡӦ�õĸ�·�������classes������WEB-INF/class�£����ӦJar������WEB-INF/libĿ¼�£��˴�����Ҫ�޸�
			// the host name is used to distinguish different temporary work
			// path,
			// eg. work/tomcat/localhost/xxxxxx/��work/tomcat/online/yyyyyy
			tomcat.setHostname("localhost");

			// ���ù���Ŀ¼,��ʵûʲô��,tomcat��Ҫʹ�����Ŀ¼����дһЩ����
			// Tomcat needs a directory for temp files.
			tomcat.setBaseDir(baseDir);

			// ����webappĿ¼
			String appBase = baseDir + File.separator + "WebContent";
			File f = new File(appBase + "/WEB-INF/web.xml");
			if (!f.exists()) {
				appBase = baseDir + File.separator + "webapp";
				f = new File(appBase + "/WEB-INF/web.xml");
				if (!f.exists()) {
					throw new Exception("�޷��ҵ�ҳ��Ŀ¼" + appBase);
				}
			}
			System.out.println("Finded Base Web Root is :" + appBase);

			// ���ϵͳ��ʹ��APR����The Apache Portable Runtime������Ҫע��AprLifecycleListener
			// StandardServer server = (StandardServer) tomcat.getServer();
			// Add AprLifecycleListener
			// AprLifecycleListener listener = new AprLifecycleListener();
			// server.addLifecycleListener(listener);
			// ע��رն˿��Խ��йر�
			// ���������Լ�����һ���˿ڽ��й����������ﲻע��8005
			// tomcat.getServer().setPort(8005);

			// Ϊ���Զ���MIMETYPE��֧�ֲ�ͬ�����ļ������أ���ͳ�Ͼ���ȫ��Ĭ��web.xml�н��ж���
			// ����Tomcat 7��ʹ��Ƕ��ʽ��ʽ����ʹ��Tomcat��ʱ��Ĭ��ǿ�Ʋ�ʹ��ȫ��web.xml
			// ʹ���Լ���ʼ��ContextConfig��ʹ��addLifecycleListener�������ķ�ʽ�ᵼ����������
			// ����Tomcat�������е�noDefaultWebXmlPath�Ľ��Ҳ��һ��
			// ���շ�����ֱ�ӵķ����ǵ���StandardContext��addMimeMapping��������������
			// Ϊ�˽����ӱ��ֱ�ۺ�������⣬�ڱ�����ʹ��WebXml�����ȫ��web.xml
			// ��getMimeMappings()�õ������ͣ�ȫ�����뵽StandardContext��
			// ����ڳ�����ʹ��koalserverbase���ʱ�������ڸ�·����confĿ¼�м���һ��web.xml
			// ��Ȼ�������Ը��ļ��Ƿ���ڽ����жϣ�������ļ������ڣ��򲻻���н���
			/** �����ȫ��web.xml �Ҿ����õ� ������ļ� */
			File globalWebXml = new File(baseDir+"/conf/web.xml");
			Map<String, String> mimeMappings = parseMimeMappingFromWebXml(globalWebXml.getCanonicalPath());

			// 2.4 context
			Context context = tomcat.addWebapp("/", new File(baseDir).getAbsolutePath());
			StandardContext standardContext = (StandardContext) context;

			// 2.4.1 ����mimeMappings
			if (mimeMappings != null) {
				for (String key : mimeMappings.keySet()) {
					standardContext.addMimeMapping(key, mimeMappings.get(key));
				}
			}

			// 2.4.2 ���ó�ʱ
			if (sessionTimeOut > 0) {
				standardContext.setSessionTimeout(sessionTimeOut);
			}
			// 2.4.3 �����Ƿ����session
			standardContext.setCrossContext(shareSession);

			// 2.4.3 ���δʵ�����л��������쳣java.io.WriteAbortedException
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
	 * ����Tomcat����
	 * 
	 * @param httpPort
	 *            http�˿�
	 * @throws Exception
	 */
	protected void setHttpServer(int httpPort) throws Exception {
		if (tomcat == null) {
			throw new Exception("tomcatδ��ʼ�����޷�����http ����");
		}
		// 2.5. Connector - HTTP
		// ��һ��Ĭ�ϵģ�����Ҫ�Լ�����
		Connector httpConnector = tomcat.getConnector();
		httpConnector.setPort(httpPort);
		httpConnector.setMaxPostSize(maxPostSize);
		httpConnector.setURIEncoding(uriEncoding);
		for (String attKey : tomcatAttributes.keySet()) {
			httpConnector.setAttribute(attKey, tomcatAttributes.get(attKey));
		}
		// ��IP��ַ
		IntrospectionUtils.setProperty(httpConnector, "address", httpBindIP);
		this.httpPort = httpPort;
	}

	/**
	 * ��������
	 * 
	 * @throws Exception
	 */
	public void startServer() throws Exception {
		if (tomcat == null) {
			throw new Exception("Tomcat��δ��ʼ�����޷���������");
		}

		// tomcat ����
		tomcat.start();
		if (this.isConnectorListening(httpPort)) {
			System.out.println("HTTP Service is start successfully: {" + httpBindIP + ":" + httpPort + "}");
		}
		tomcat.getServer().await();
	}

	/**
	 * ֹͣ����
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
