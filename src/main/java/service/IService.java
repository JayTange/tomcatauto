package service;

public interface IService {

	/**
	 * 服务启动
	 * @throws Exception
	 */
	public void start() throws Exception;
	/**
	 * 服务终止
	 * @throws Exception
	 */
	public void stop() throws Exception;
}
