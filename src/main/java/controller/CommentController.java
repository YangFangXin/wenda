package controller;

import java.text.SimpleDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import async.EventProducer;
import async.EventType;
import async.StandardEvent;
import model.Comment;
import model.EntityType;
import model.HostHolder;
import model.Question;
import service.CommentService;
import service.ConfigService;
import service.QuestionService;
import util.DateUtil;
import util.IdResolver;
import util.JSONUtil;

@Controller
public class CommentController {
	
	//private static final Logger logger = LoggerFactory.getLogger(CommentController.class); 
	
	@Autowired
	HostHolder hostHolder;
	
	@Autowired
	CommentService commentService;
	
	@Autowired
	EventProducer eventProducer;
	
	@Autowired
	QuestionService questionService;
	
	@Autowired
	ConfigService configService;
	
	
	
	@RequestMapping(value = "/comment/question/{questionId}/img" ,method = RequestMethod.POST)
	@ResponseBody
	public String uploadCommentImg(@PathVariable("questionId") String questionIdStr,
							@RequestParam("commentId") int commentId,
							@RequestParam("offset") int offset,
							@RequestParam("commentImg") MultipartFile commentImg){
		//Date before = new Date();
		int questionId = IdResolver.resolveId(questionIdStr);
		if(commentImg!=null)
		{
			String url = commentService.uploadQuestionCommetImg(questionId, commentImg, commentId, offset);
			//Date after = new Date();
			//System.out.println(Thread.currentThread().getId()+":"+(after.getTime()-before.getTime()));
			if(url!=null)
				return "{}";
			else
				return JSONUtil.getJSONString("error", "上传图片错误");
		}
		else return "{}";
	}
	
	
	@RequestMapping(value = "/comment/question/{id}" ,method = RequestMethod.POST)
	@ResponseBody
	public String addQuestionComment(@PathVariable("id") String idStr,
			@RequestParam("content") String content,
			@RequestParam("filesCount") int filesCount){
		
		int id = IdResolver.resolveId(idStr);
		if(hostHolder.getUser()==null)
		{
			return JSONUtil.getJSONString(JSONUtil.UNLOGIN);
		}else if(StringUtils.isEmpty(content))
		{
			return JSONUtil.getJSONString(JSONUtil.EMPTY_CONTENT,"内容为空");
		}
		else
		{
			Comment comment = new Comment();
			comment.setContent(content);
			comment.setCreateDate(DateUtil.now());
			comment.setEntityId(id);
			comment.setEntityType(EntityType.QUESTION);
			comment.setStatus(Comment.NORMAL_STATUS);
			comment.setUserId(hostHolder.getUser().getId());
			comment.setImgCount(filesCount);
			int res = commentService.addComment(comment);
			//失败
			if(res<0)
			{
				return JSONUtil.getJSONString(JSONUtil.FAIL, "添加失败");
			}else
			{
				//这里需要一个事务
				Question question = questionService.getQuestion(id);
				questionService.updateQuestionCommentCount(id, commentService.getCommentsCountOfQuestion(id));
				//
				
				int questionOwnerId = question.getUserId();
				if(questionOwnerId==hostHolder.getUser().getId());
				else
				{
					SimpleDateFormat s = new SimpleDateFormat();
					eventProducer.fireEvent(new StandardEvent()
							.setActorId(hostHolder.getUser().getId())
							.setEntityId(id)
							.setEntityOwnerId(questionOwnerId)
							.setEntityType(EntityType.QUESTION)
							.setType(EventType.RAISE_COMMENT_EVENT)
							.setInfomation("commentId", String.valueOf(res))
							.setInfomation("createDate", s.format(DateUtil.now())));
				}
				
				//返回添加成功和commentId
				return JSONUtil.getJSONString(JSONUtil.SUCCESS,String.valueOf(res));
			}
		}
	}
	
	@RequestMapping(value = "/comment/comment/{id}" ,method = RequestMethod.POST)
	@ResponseBody
	public String addCommentComment(@PathVariable("id") String idStr,@RequestParam("content") String content)
	{
		
		int id = IdResolver.resolveId(idStr);
		if(hostHolder.getUser()==null)
		{
			return JSONUtil.getJSONString(JSONUtil.UNLOGIN);
		}else if(StringUtils.isEmpty(content))
		{
			return JSONUtil.getJSONString(JSONUtil.EMPTY_CONTENT,"内容为空");
		}
		else
		{
			Comment comment = new Comment();
			comment.setContent(content);
			comment.setCreateDate(DateUtil.now());
			comment.setEntityId(id);
			comment.setEntityType(EntityType.COMMENT);
			comment.setStatus(Comment.NORMAL_STATUS);
			comment.setUserId(hostHolder.getUser().getId());
			
			int res = commentService.addComment(comment);
			//失败
			if(res<0)
			{
				return JSONUtil.getJSONString(JSONUtil.FAIL, "添加失败");
			}else
			{
				int commentOwnerId = commentService.getCommentById(id).getUserId();
				if(commentOwnerId==hostHolder.getUser().getId());
				else
				{
					eventProducer.fireEvent(new StandardEvent()
							.setActorId(hostHolder.getUser().getId())
							.setEntityId(id)
							.setEntityOwnerId(commentOwnerId)
							.setEntityType(EntityType.COMMENT)
							.setType(EventType.RAISE_COMMENT_EVENT));
				}
				return JSONUtil.getJSONString(JSONUtil.SUCCESS);
			}
		}
	}
}
