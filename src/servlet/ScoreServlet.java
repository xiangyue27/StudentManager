package servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Course;
import model.Page;
import model.Score;
import model.Student;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.lizhou.exception.FileFormatException;
import com.lizhou.exception.NullFileException;
import com.lizhou.exception.ProtocolException;
import com.lizhou.exception.SizeException;
import com.lizhou.fileload.FileUpload;

import dao.CourseDao;
import dao.ScoreDao;
import dao.SelectedCourseDao;
import dao.StudentDao;

public class ScoreServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -153736421631912372L;
	
	

	public void doGet(HttpServletRequest request,HttpServletResponse response) throws IOException{
		doPost(request, response);
	}
	public void doPost(HttpServletRequest request,HttpServletResponse response) throws IOException{
		String method = request.getParameter("method");
		if("toScoreListView".equals(method)){
			try {
				request.getRequestDispatcher("view/scoreList.jsp").forward(request, response);
			} catch (ServletException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if("AddScore".equals(method)){
			addScore(request,response);
		}else if("ScoreList".equals(method)){
			getScoreList(request,response);
		}else if("EditScore".equals(method)){
			editScore(request,response);
		}else if("DeleteScore".equals(method)){
			deleteScore(request,response);
		}else if("ImportScore".equals(method)){
			importScore(request,response);
		}else if("ExportScoreList".equals(method)){
			exportScore(request,response);
		}
	}
	
	private void exportScore(HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
		int studentId = request.getParameter("studentid") == null ? 0 : Integer.parseInt(request.getParameter("studentid").toString());
		int courseId = request.getParameter("courseid") == null ? 0 : Integer.parseInt(request.getParameter("courseid").toString());
		//获取当前登录用户类型
		int userType = Integer.parseInt(request.getSession().getAttribute("userType").toString());
		if(userType == 2){
			//如果是学生，只能查看自己的信息
			Student currentUser = (Student)request.getSession().getAttribute("user");
			studentId = currentUser.getId();
		}
		Score score = new Score();
		score.setStudentId(studentId);
		score.setCourseId(courseId);
		try {
			response.setHeader("Content-Disposition", "attachment;filename="+URLEncoder.encode("成绩表.xls", "UTF-8"));
			response.setHeader("Connection", "close");
			response.setHeader("Content-Type", "application/octet-stream");
			ServletOutputStream outputStream = response.getOutputStream();
			ScoreDao scoreDao = new ScoreDao();
			List<Map<String, Object>> scoreList = scoreDao.getScoreList(score);
			scoreDao.closeCon();
			HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
			HSSFSheet createSheet = hssfWorkbook.createSheet("成绩列表");
			HSSFRow createRow = createSheet.createRow(0);
			createRow.createCell(0).setCellValue("学生");
			createRow.createCell(1).setCellValue("课程");
			createRow.createCell(2).setCellValue("成绩");
			createRow.createCell(3).setCellValue("备注");
			//实现将数据装入到excel文件中
			int row = 1;
			for(Map<String, Object> entry:scoreList){
				createRow = createSheet.createRow(row++);
				createRow.createCell(0).setCellValue(entry.get("studentName").toString());
				createRow.createCell(1).setCellValue(entry.get("courseName").toString());
				createRow.createCell(2).setCellValue(new Double(entry.get("score")+""));
				createRow.createCell(3).setCellValue(entry.get("remark")+"");
			}
			hssfWorkbook.write(outputStream);
			outputStream.flush();
			outputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("static-access")
	private void importScore(HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
		FileUpload fileUpload = new FileUpload(request);
		fileUpload.setFileFormat("xls");
		fileUpload.setFileFormat("xlsx");
		fileUpload.setFileSize(2048);
		response.setCharacterEncoding("UTF-8");
		try {
			InputStream uploadInputStream = fileUpload.getUploadInputStream();
			HSSFWorkbook hssfWorkbook = new HSSFWorkbook(uploadInputStream);
			HSSFSheet sheetAt = hssfWorkbook.getSheetAt(0);
			int count = 0;
			String errorMsg = "";
			StudentDao studentDao = new StudentDao();
			CourseDao courseDao = new CourseDao();
			ScoreDao scoreDao = new ScoreDao();
			SelectedCourseDao selectedCourseDao = new SelectedCourseDao();
			for(int rowNum = 1; rowNum <= sheetAt.getLastRowNum(); rowNum++){
				HSSFRow row = sheetAt.getRow(rowNum);
				HSSFCell cell = row.getCell(0);
				
				//获取第0列，学生id
				if(cell == null){
					errorMsg += "第" + rowNum + "行学生id不存在！\n";
					continue;
				}
				if(cell.getCellType() != cell.CELL_TYPE_NUMERIC){
					errorMsg += "第" + rowNum + "行学生id类型不是整数！\n";
					continue;
				}
				int studentId = new Double(cell.getNumericCellValue()).intValue();
				
				//获取第1列，课程id
				cell = row.getCell(1);
				if(cell == null){
					errorMsg += "第" + rowNum + "行课程id不存在！\n";
					continue;
				}
				if(cell.getCellType() != cell.CELL_TYPE_NUMERIC){
					errorMsg += "第" + rowNum + "行课程id不是整数！\n";
					continue;
				}
				int courseId = new Double(cell.getNumericCellValue()).intValue();
				
				//获取第2列，成绩
				cell = row.getCell(2);
				if(cell == null){
					errorMsg += "第" + rowNum + "行成绩不存在！\n";
					continue;
				}
				if(cell.getCellType() != cell.CELL_TYPE_NUMERIC){
					errorMsg += "第" + rowNum + "行成绩类型不是数字！\n";
					continue;
				}
				double scoreValue = cell.getNumericCellValue();
				
				//获取第3列，备注
				cell = row.getCell(3);
				String remark = null;
				if(cell != null){
					remark = cell.getStringCellValue();
				}
				Student student = studentDao.getStudent(studentId);
				if(student == null){
					errorMsg += "第" + rowNum + "行学生id不存在！\n";
					continue;
				}
				Course course = courseDao.getCourse(courseId);
				if(course == null){
					errorMsg += "第" + rowNum + "行课程id不存在！\n";
					continue;
				}
				if(!selectedCourseDao.isSelected(studentId, courseId)){
					errorMsg += "第" + rowNum + "行课程该同学未选，不合法！\n";
					continue;
				}
				if(scoreDao.isAdd(studentId, courseId)){
					errorMsg += "第" + rowNum + "行成绩已经被添加，请勿重复添加！\n";
					continue;
				}
				Score score = new Score();
				score.setCourseId(courseId);
				score.setRemark(remark);
				score.setScore(scoreValue);
				score.setStudentId(studentId);
				if(scoreDao.addScore(score)){
					count++;
				}
			}
			errorMsg += "成功录入" + count + "条成绩信息！";
			studentDao.closeCon();
			courseDao.closeCon();
			selectedCourseDao.closeCon();
			scoreDao.closeCon();
			try {
				response.getWriter().write("<div id='message'>"+errorMsg+"</div>");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			try {
				response.getWriter().write("<div id='message'>上传协议错误！</div>");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}catch (NullFileException e1) {
			// TODO: handle exception
			try {
				response.getWriter().write("<div id='message'>上传的文件为空!</div>");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			e1.printStackTrace();
		}
		catch (SizeException e2) {
			// TODO: handle exception
			try {
				response.getWriter().write("<div id='message'>上传文件大小不能超过"+fileUpload.getFileSize()+"！</div>");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			e2.printStackTrace();
		}
		catch (IOException e3) {
			// TODO: handle exception
			try {
				response.getWriter().write("<div id='message'>读取文件出错！</div>");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			e3.printStackTrace();
		}
		catch (FileFormatException e4) {
			// TODO: handle exception
			try {
				response.getWriter().write("<div id='message'>上传文件格式不正确，请上传 "+fileUpload.getFileFormat()+" 格式的文件！</div>");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			e4.printStackTrace();
		}
		catch (FileUploadException e5) {
			// TODO: handle exception
			try {
				response.getWriter().write("<div id='message'>上传文件失败！</div>");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			e5.printStackTrace();
		}
	}
	private void deleteScore(HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
		int id = Integer.parseInt(request.getParameter("id"));
		ScoreDao scoreDao = new ScoreDao();
		String msg = "success";
		if(!scoreDao.deleteScore(id)){
			msg = "error";
		}
		scoreDao.closeCon();
		try {
			response.getWriter().write(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void editScore(HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
		int id = Integer.parseInt(request.getParameter("id"));
		int studentId = request.getParameter("studentid") == null ? 0 : Integer.parseInt(request.getParameter("studentid").toString());
		int courseId = request.getParameter("courseid") == null ? 0 : Integer.parseInt(request.getParameter("courseid").toString());
		Double scoreNum = Double.parseDouble(request.getParameter("score"));
		String remark = request.getParameter("remark");
		Score score = new Score();
		score.setId(id);
		score.setCourseId(courseId);
		score.setStudentId(studentId);
		score.setScore(scoreNum);
		score.setRemark(remark);
		ScoreDao scoreDao = new ScoreDao();
		String ret = "success";
		if(!scoreDao.editScore(score)){
			ret = "error";
		}
		try {
			response.getWriter().write(ret);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void getScoreList(HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
		int studentId = request.getParameter("studentid") == null ? 0 : Integer.parseInt(request.getParameter("studentid").toString());
		int courseId = request.getParameter("courseid") == null ? 0 : Integer.parseInt(request.getParameter("courseid").toString());
		Integer currentPage = request.getParameter("page") == null ? 1 : Integer.parseInt(request.getParameter("page"));
		Integer pageSize = request.getParameter("rows") == null ? 999 : Integer.parseInt(request.getParameter("rows"));
		Score score = new Score();
		//获取当前登录用户类型
		int userType = Integer.parseInt(request.getSession().getAttribute("userType").toString());
		if(userType == 2){
			//如果是学生，只能查看自己的信息
			Student currentUser = (Student)request.getSession().getAttribute("user");
			studentId = currentUser.getId();
		}
		score.setCourseId(courseId);
		score.setStudentId(studentId);
		ScoreDao scoreDao = new ScoreDao();
		List<Score> courseList = scoreDao.getScoreList(score, new Page(currentPage, pageSize));
		int total = scoreDao.getScoreListTotal(score);
		scoreDao.closeCon();
		response.setCharacterEncoding("UTF-8");
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("total", total);
		ret.put("rows", courseList);
		try {
			String from = request.getParameter("from");
			if("combox".equals(from)){
				response.getWriter().write(JSONArray.fromObject(courseList).toString());
			}else{
				response.getWriter().write(JSONObject.fromObject(ret).toString());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void addScore(HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
		int studentId = request.getParameter("studentid") == null ? 0 : Integer.parseInt(request.getParameter("studentid").toString());
		int courseId = request.getParameter("courseid") == null ? 0 : Integer.parseInt(request.getParameter("courseid").toString());
		Double scoreNum = Double.parseDouble(request.getParameter("score"));
		String remark = request.getParameter("remark");
		Score score = new Score();
		score.setCourseId(courseId);
		score.setStudentId(studentId);
		score.setScore(scoreNum);
		score.setRemark(remark);
		ScoreDao scoreDao = new ScoreDao();
		if(scoreDao.isAdd(studentId, courseId)){
			try {
				response.getWriter().write("added");
				scoreDao.closeCon();
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String ret = "success";
		if(!scoreDao.addScore(score)){
			ret = "error";
		}
		try {
			response.getWriter().write(ret);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
