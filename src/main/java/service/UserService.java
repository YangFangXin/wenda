package service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import dao.LoginTicketDAO;
import dao.MybatisSqlSessionFactory;
import dao.UserDAO;
import model.LoginTicket;
import model.User;
import util.DateUtil;
import util.MD5Util;

@Service
public class UserService {
	
	@Autowired
	SensitiveWordsService sensitiveWordsService;
	
	private static int MIN_PASSWORD_LENGTH = 2;
	
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	
	
	public Map<String,String> login(String username,String password,boolean rememberMe)
	{
		Map<String,String> map = new HashMap<String,String>();
		if(StringUtils.isEmpty(username)){
			map.put("error", "用户名不能为空");
			return map;
		}
		if(StringUtils.isEmpty(password)){
			map.put("error", "密码不能为空");
			return map;
		}
		
		User user = this.getUser(username);
		if(user==null)
		{
			map.put("error", "用户名或密码错误");
			return map;
		}
		
		if(!MD5Util.getMD5(password+user.getSalt()).equals(user.getPassword())){
			map.put("error", "用户名或密码错误");
			return map;
		}
		
		String ticket = this.addTicket(user.getId(),rememberMe);
		map.put("ticket", ticket);
		return map;
		
	}
	
	public void logout(String ticket)
	{
		SqlSession session = MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		LoginTicketDAO loginTicketDAO;
		try{
			loginTicketDAO = session.getMapper(LoginTicketDAO.class);
			loginTicketDAO.updateStatus(-1, ticket);
			session.commit();
		}catch(Exception e)
		{
			logger.error("登出错误 "+e.getMessage());
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
	}
	
	
	public Map<String,String> register(String username,String password,boolean rememberMe)
	{
		Map<String,String> map = new HashMap<String,String>();
		if(StringUtils.isEmpty(username)){
			map.put("error", "用户名不能为空");
			return map;
		}
		if(StringUtils.isEmpty(password)){
			map.put("error", "密码不能为空");
			return map;
		}
		if(password.length()<MIN_PASSWORD_LENGTH)
		{
			map.put("error", "密码长度不能小于" +MIN_PASSWORD_LENGTH);
			return map;
		}
		
		User user = this.getUser(username);
		if(user!=null)
		{
			map.put("error", "用户名已经被注册");
			return map;
		}
		username = HtmlUtils.htmlEscape(username);
		user = new User();
		user.setUsername(username);
		user.setSalt(UUID.randomUUID().toString().substring(0,5));
		user.setHeadUrl(User.DEFAULT_HEAD_URL);
		user.setPassword(MD5Util.getMD5(password+user.getSalt()));
		if(!this.addUser(user))
		{
			map.put("error", "用户注册错误");
			return map;
		}
		
		String ticket = this.addTicket(user.getId(),rememberMe);
		map.put("ticket", ticket);
		
		return map;
	}
	
	//根据userId下发一个ticket
	private String addTicket(int userId,boolean rememberMe){
		SqlSession session = MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		LoginTicket loginTicket = new LoginTicket();
		loginTicket.setUserId(userId);
		loginTicket.setStatus(0);
		loginTicket.setTicket(UUID.randomUUID().toString().replaceAll("-",""));
		Date now =DateUtil.now();
		if(rememberMe)
			now.setTime(LoginTicket.EXPIRED_TIME_MILLISECONDS + now.getTime());
		else
			now.setTime(LoginTicket.EXPIRED_TIME_MILLISECONDS_IF_NOTREMEBERME + now.getTime());
		loginTicket.setExpired(now);
		LoginTicketDAO loginTicketDAO;
		
		try{
			loginTicketDAO = session.getMapper(LoginTicketDAO.class);
			loginTicketDAO.addTicket(loginTicket);
			session.commit();
		}catch(Exception e)
		{
			logger.error("下发ticket错误 "+e.getMessage());
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
		return loginTicket.getTicket();
	}
	
	
	private boolean addUser(User user){
		SqlSession session = MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		UserDAO userDAO;
		try{
			userDAO = session.getMapper(UserDAO.class);
			userDAO.addUser(user);
			session.commit();
		}catch(Exception e)
		{
			logger.error("添加用户错误 "+e.getMessage());
			return false;
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
		return true;
	}
	
	
	private User getUser(String username){
		SqlSession session = MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		UserDAO userDAO;
		User user = null;
		try{
			userDAO = session.getMapper(UserDAO.class);
			user = userDAO.selectUserByUsername(username);
			session.commit();
		}catch(Exception e)
		{
			logger.error("根据username查询用户错误 "+e.getMessage());
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
		return user;
	}
	
	public User getUser(int id){
		SqlSession session = MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		UserDAO userDAO;
		User user = null;
		try{
			userDAO = session.getMapper(UserDAO.class);
			user = userDAO.selectUserById(id);
			session.commit();
		}catch(Exception e)
		{
			logger.error("根据Id查询用户错误 "+e.getMessage());
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
		return user;
	}
}
