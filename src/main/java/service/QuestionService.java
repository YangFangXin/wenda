package service;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import dao.MybatisSqlSessionFactory;
import dao.QuestionDAO;
import model.Question;

@Service
public class QuestionService{
	
	@Autowired
	private SensitiveWordsService sensitiveWordsService;
	
	private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);
	
	public int getQuestionCount()
	{
		SqlSession session=MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		QuestionDAO questionDAO;
		int result = -1;
		try{
			questionDAO = session.getMapper(QuestionDAO.class);
			result= questionDAO.getQuestionCount();
		}catch(Exception e)
		{
			logger.error("查询问题总数错误 "+e.getMessage());
			return -1;
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
		return result;
	}
	
	public int updateQuestionCommentCount(int questionId,int commentCount)
	{
		SqlSession session=MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		QuestionDAO questionDAO;
		int result = -1;
		try{
			questionDAO = session.getMapper(QuestionDAO.class);
			result= questionDAO.updateCommentCout(questionId, commentCount);
			session.commit();
		}catch(Exception e)
		{
			logger.error("更新问题评论数错误 "+e.getMessage());
			return -1;
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
		return result;
	}
	
	
	public List<Question> getLatestQuestion(int userId,int offset,int limit){
		SqlSession session=MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		List<Question> list = null;
		QuestionDAO questionDAO;
		try{
			questionDAO = session.getMapper(QuestionDAO.class);
			list= questionDAO.selectLatestQuestions(userId, offset, limit);
			session.commit();
		}catch(Exception e)
		{
			logger.error("获取最近问题错误 "+e.getMessage());
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
		return list;
	}

	/*
	 * 返回值：添加成功返回0,否则返回-1
	 */
	public int addQuestion(Question question)
	{
		SqlSession session=MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		QuestionDAO questionDAO;
		try{
			questionDAO = session.getMapper(QuestionDAO.class);
			
			//敏感词过滤
			question.setContent(sensitiveWordsService
					.filterWords(HtmlUtils.htmlEscape(question.getContent())));
			question.setTitle(sensitiveWordsService
					.filterWords(HtmlUtils.htmlEscape(question.getTitle())));
			
			
			questionDAO.addQuestion(question);
			session.commit();
		}catch(Exception e)
		{
			logger.error("添加问题错误  "+e.getMessage());
			return -1;
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
		return 0;
	}
	
	public Question getQuestion(int id){
		SqlSession session=MybatisSqlSessionFactory.getSqlSessionFactory().openSession();
		Question question = null;
		QuestionDAO questionDAO;
		try{
			questionDAO = session.getMapper(QuestionDAO.class);
			question= questionDAO.selectQuestion(id);
		}catch(Exception e)
		{
			logger.error("根据Id获取具体问题错误 "+e.getMessage());
		}finally
		{
			if(session!=null)
			{
				session.close();
			}
		}
		return question;
	}
}
