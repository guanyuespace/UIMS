package guanyue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @version 1.0
 * @author Guan Yue
 * @time 2017年11月29日 下午12:31:25
 *
 */
public class Test {
	//
	private static RequestConfig config = RequestConfig.custom().setConnectTimeout(1000)
			.setCookieSpec(CookieSpecs.DEFAULT).build();
	private static CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(config).build();

	// j_spring_security_check
	private static String loginUrl = "http://uims.jlu.edu.cn/ntms/j_spring_security_check";

	//
	private static int stuID = 0, stuTERM = 0;

	private static void menu() {
		// TODO Auto-generated method stub
		System.out.println(
				"功能如下：\r\n(提示请输入小括号内字母)\r\n\t评教(TE)：查询已评教记录或进行一键评教\r\n\t最近成绩查询(SS)：*****\r\n\t课表查询(CC):************\r\n\t课程查询(JC):*****************\r\n\t学期成绩查询(CS):******\r\n\t课程分数比例(CR):*******\r\n\t菜单查看(M)\r\n\t退出(Q)");

	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		Scanner scanner = null;
		try {
			String username = null, passwd = null, url = loginUrl;
			int num = 0, term = 0,asId=0;
			scanner = new Scanner(System.in);
			if (args.length != 2) {
				System.out.print("输入教学号：");
				username = scanner.nextLine().trim();
				System.out.print("输入密码：");
				passwd = scanner.nextLine().trim();
			} else {
				username = args[0];
				passwd = args[1];
			}
			passwd = md5(username, passwd);
			uimsLogin(url, username, passwd);

			// start do something here .....
			menu();
			boolean flag = true;
			while (flag) {
				System.out.print("功能选择：");
				String ss = scanner.nextLine().trim().toUpperCase();
				switch (ss) {
				case "TE":
					uimsTeachingEvaluation();
					break;
				case "SS":
					System.out.print("输入查询最近科目数：");
					num = Integer.parseInt(scanner.nextLine().trim());
					if (num <= 0)
						num = 15;
					uimsScore(num, false);
					break;
				case "CC":
					System.out.printf("(课表查询)输入待查询学期(当前学期：%1$3d)：", stuTERM);
					term = Integer.parseInt(scanner.nextLine().trim());
					if (term > stuTERM)
						term = stuTERM;
					uimsCourse(term);
					break;
				case "JC":
					System.out.printf("(课程查询)输入待查询学期(当前学期：%1$3d)：", stuTERM);
					term = Integer.parseInt(scanner.nextLine().trim());
					if (term > stuTERM)
						term = stuTERM;
					uimsjustCourse(term);
					break;
				case "CR":
					System.out.printf("(分数比例查询)输入待查询课程内部ID：");
					asId = Integer.parseInt(scanner.nextLine().trim());
					uimsScoreRate(asId);
					break;
				case "CS":
					System.out.printf("(分数查询)输入待查询学期(当前学期：%1$3d)：", stuTERM);
					term = Integer.parseInt(scanner.nextLine().trim());
					if (term > stuTERM)
						term = stuTERM;
					uimsScoreCourse(term);
					break;
				case "Q":
					flag = false;
					break;
			
				default:
					menu();
					break;
				}
			}

			//
			uimsLogout();

		} finally {
			if (scanner != null)
				scanner.close();
			if (client != null)
				client.close();
		}
	}

	private static String md5(String username, String passwd) {
		// TODO Auto-generated method stub
		passwd = "UIMS" + username + passwd;
		String md5str = "";
		try {
			// 1 创建一个提供信息摘要算法的对象，初始化为md5算法对象
			MessageDigest md = MessageDigest.getInstance("MD5");

			// 2 将消息变成byte数组
			byte[] passwdbytes = passwd.getBytes();

			// 3 计算后获得字节数组,这就是那128位了
			byte[] buff = md.digest(passwdbytes);

			// 4 把数组每一字节（一个字节占八位）换成16进制连成md5字符串
			md5str = bytesToHex(buff);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return md5str;
	}

	private static String bytesToHex(byte[] buff) {
		// TODO Auto-generated method stub
		StringBuffer md5str = new StringBuffer();
		// 把数组每一字节换成16进制连成md5字符串
		int digital;
		for (int i = 0; i < buff.length; i++) {
			digital = buff[i];
			if (digital < 0) {
				digital += 256;
			}
			if (digital < 16) {
				md5str.append("0");
			}
			md5str.append(Integer.toHexString(digital));
		}
		return md5str.toString();
	}

	/**
	 * 登陆UIMS
	 * 
	 * @param url:登陆地址
	 * @param username：登陆名
	 * @param passwd：登陆密码（MD5）
	 */
	private static void uimsLogin(String url, String username, String passwd) {
		// TODO Auto-generated method stub
		try {
			HttpPost post = new HttpPost(url);
			post.addHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			post.addHeader("Accept-Encoding", "gzip, deflate");
			post.addHeader("Accept-Language", "zh-CN,zh;q=0.8");
			post.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("j_username", username));// TxtUserName
			nameValuePairs.add(new BasicNameValuePair("j_password", passwd));// TxtPassword
			nameValuePairs.add(new BasicNameValuePair("mousePath", ""));
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));
			HttpResponse response = client.execute(post);

			System.out.println("开始登陆:\t" + response.getStatusLine());
			String login_suss = response.getFirstHeader("location").getValue().trim().split(";")[0];
			System.out.println("中间跳转 ---> " + login_suss);
			post.abort();

			HttpGet get = new HttpGet(login_suss);
			get.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
			response = client.execute(get);
			System.out.println("跳转登陆:\t" + response.getStatusLine());
			get.abort();

			HttpPost post2 = new HttpPost("http://uims.jlu.edu.cn/ntms/action/getCurrentUserInfo.do");
			response = client.execute(post2);
			System.out.println("获取用户信息：\t" + response.getStatusLine());
			JSONObject raw = new JSONObject(EntityUtils.toString(response.getEntity()));
			post2.abort();

			String userId = raw.getString("userId");
			stuID = Integer.parseInt(userId.trim());

			String teachingTerm = raw.getJSONObject("defRes").getString("teachingTerm");
			stuTERM = Integer.parseInt(teachingTerm.trim());

			String nickName = raw.getString("nickName");
			String date = raw.getString("sysTime");
			String loginName = raw.getString("loginName");
			String groupName = raw.getJSONArray("groupsInfo").getJSONObject(0).getString("groupName");
			System.out.println("当前时间：" + date + "\t教学号：" + loginName + "\t当前学期（内部表示）：" + teachingTerm + "\t\t内部ID:"
					+ userId + "\t姓名：" + nickName + "\t职位：" + groupName);
		} catch (IOException | ParseException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException ie) {
				// TODO Auto-generated catch block
				ie.printStackTrace();
			}
		}
	}

	/**
	 * UIMS一键评教,查看已评记录
	 */
	private static void uimsTeachingEvaluation() {
		// TODO Auto-generated method stub
		BufferedInputStream bufferedInputStream = null;
		try {
			// 尚未评教记录查询
			String url = "http://uims.jlu.edu.cn/ntms/service/res.do";
			HttpPost post = new HttpPost(url);
			post.addHeader("Content-Type", "application/json");
			post.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
			String strs = "{\"tag\":\"student@evalItem\",\"branch\":\"self\",\"params\":{\"blank\":\"Y\"}}";
			post.setEntity(new StringEntity(strs));

			HttpResponse response = client.execute(post);
			System.out.println("查询未评记录:\t" + response.getStatusLine());
			bufferedInputStream = new BufferedInputStream(response.getEntity().getContent(), 102400);
			StringBuilder builder = new StringBuilder();
			int n = 0;
			byte[] bs = new byte[102400];
			while ((n = bufferedInputStream.read(bs)) != -1) {
				builder.append(new String(bs, 0, n));
			}
			post.abort();

			JSONObject raw = new JSONObject(builder.toString());
			JSONArray value = raw.getJSONArray("value");
			for (int i = 0; i < value.length(); i++) {
				JSONObject techInfo = value.getJSONObject(i);
				JSONObject target = techInfo.getJSONObject("target");
				String teacherName = target.getString("name");
				String teacherPersonID = target.getString("personId");
				String teacherSchool = target.getJSONObject("school").getString("schoolName");
				String subject = techInfo.getJSONObject("targetClar").getString("notes");
				JSONObject evalActTime = techInfo.getJSONObject("evalActTime").getJSONObject("evalTime");
				String start = evalActTime.getString("dateStart");
				String stop = evalActTime.getString("dateStop");
				String evalItemId = techInfo.getString("evalItemId");
				System.out
						.println("学院：" + teacherSchool + "\t教师：" + teacherName + "\t教师ID：" + teacherPersonID + "\t所教科目："
								+ subject + "\t项目ID:" + evalItemId + "\t(评教开始时间：" + start + " 评教结束时间" + stop + " )");

				// HttpPost post2 = new HttpPost("http://uims.jlu.edu.cn/ntms/service/res.do");
				// strs = "{\"tag\":\"get@EvalItem\",\"params\":{\"evalItemId\":\"" + evalItemId
				// + "\"}}";
				// post2.addHeader("Content-Type", "application/json");
				// post2.setEntity(new StringEntity(strs));
				// response = client.execute(post2);
				// System.out.println("选定该待评教师：\t"+response.getStatusLine());
				// System.out.println(EntityUtils.toString(response.getEntity()));
				// post2.abort();
				//
				// HttpPost post3 = new
				// HttpPost("http://uims.jlu.edu.cn/ntms/action/get-message-count.do");
				// response = client.execute(post3);
				// System.out.println("选定该待评教师：\t"+response.getStatusLine());
				// System.out.println(EntityUtils.toString(response.getEntity()));
				// post3.abort();

				HttpPost post4 = new HttpPost("http://uims.jlu.edu.cn/ntms/page/eval/action/eval/eval-with-answer.do");
				strs = "{\"evalItemId\":\"" + evalItemId
						+ "\",\"answers\":{\"prob11\":\"A\",\"prob12\":\"A\",\"prob13\":\"D\",\"prob14\":\"A\",\"prob15\":\"D\",\"prob21\":\"A\",\"prob22\":\"A\",\"prob23\":\"A\",\"prob31\":\"A\",\"prob32\":\"A\",\"prob41\":\"A\",\"prob42\":\"A\",\"prob43\":\"C\",\"prob51\":\"A\",\"prob52\":\"A\",\"sat6\":\"A\",\"mulsel71\":\"L\",\"advice8\":\"\"}}";
				post4.addHeader("Content-Type", "application/json");
				post4.setEntity(new StringEntity(strs));
				response = client.execute(post4);
				System.out.println("评教结果：\t" + response.getStatusLine());
				System.out.println(EntityUtils.toString(response.getEntity()));
				post4.abort();
			}
			// 查询已评教记录
			HttpPost haveEvaluated = new HttpPost("http://uims.jlu.edu.cn/ntms/service/res.do");
			strs = "{\"tag\":\"student@evalItem\",\"branch\":\"self\",\"params\":{\"done\":\"Y\"}}";
			haveEvaluated.addHeader("Content-Type", "application/json");
			haveEvaluated.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
			haveEvaluated.setEntity(new StringEntity(strs));
			response = client.execute(haveEvaluated);
			System.out.println("查询已评教记录:\t" + response.getStatusLine());
			String result = EntityUtils.toString(response.getEntity());
			haveEvaluated.abort();

			JSONObject resRaw = new JSONObject(result);
			JSONArray resValue = resRaw.getJSONArray("value");
			for (int i = 0; i < resValue.length(); i++) {
				JSONObject techInfo = resValue.getJSONObject(i);
				JSONObject target = techInfo.getJSONObject("target");
				String teacherName = target.getString("name");
				String teacherPersonID = target.getString("personId");
				String teacherSchool = target.getJSONObject("school").getString("schoolName");
				String subject = techInfo.getJSONObject("targetClar").getString("notes");
				JSONObject evalActTime = techInfo.getJSONObject("evalActTime").getJSONObject("evalTime");
				String start = evalActTime.getString("dateStart");
				String stop = evalActTime.getString("dateStop");
				String evalItemId = techInfo.getString("evalItemId");
				System.out
						.println("学院：" + teacherSchool + "\t教师：" + teacherName + "\t教师ID：" + teacherPersonID + "\t所教科目："
								+ subject + "\t项目ID:" + evalItemId + "\t(评教开始时间：" + start + " 评教结束时间" + stop + " )");
			}

		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException ie) {
				// TODO Auto-generated catch block
				ie.printStackTrace();
			}
		} finally {
			if (bufferedInputStream != null) {
				try {
					bufferedInputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * 查询成绩及比例
	 * 
	 * @param flag是否查询比例
	 * @param num查询最新的num数目科
	 */
	private static void uimsScore(int num, boolean flag) {
		// TODO Auto-generated method stub
		try {
			HttpPost scorePost = new HttpPost("http://uims.jlu.edu.cn/ntms/service/res.do");
			String params = "{\"tag\":\"archiveScore@queryCourseScore\",\"branch\":\"latest\",\"params\":{},\"rowLimit\":"
					+ num + "}";
			scorePost.addHeader("Content-Type", "application/json");
			scorePost.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.89 Safari/537.36");
			scorePost.setEntity(new StringEntity(params));
			HttpResponse response = client.execute(scorePost);
			System.out.println("成绩查询\t:" + response.getStatusLine());
			String scoreResult = EntityUtils.toString(response.getEntity());
			scorePost.abort();

			JSONObject scoreRaw = new JSONObject(scoreResult);
			JSONArray scoreValue = scoreRaw.getJSONArray("value");
			for (int i = 0; i < scoreValue.length(); i++) {
				JSONObject rawScore = scoreValue.getJSONObject(i);
				String asId = rawScore.getString("asId");// 双击显示比例
				String score = rawScore.getString("score");
				String scoreNum = rawScore.getString("scoreNum");
				String credit = rawScore.getString("credit");
				credit = String.format("%-3s", credit);
				String gpoint = rawScore.getString("gpoint");
				String dateScore = rawScore.getString("dateScore");
				String classHour = rawScore.getString("classHour");
				String termName = rawScore.getJSONObject("teachingTerm").getString("termName");
				String courName = rawScore.getJSONObject("course").getString("courName");
				courName = String.format("%-20s", courName);
				System.out.println("学期：" + termName + "\t成绩发布时间：" + dateScore + "\t学时：" + classHour + "\t学分：" + credit
						+ "\t成绩：" + score + "(" + scoreNum + ")" + "\t通过绩点：" + gpoint + "\t内部ID：" + asId + "\t课程："
						+ courName);

				// 比例查询
				if (flag) {
					uimsScoreRate(Integer.parseInt(asId.trim()));
				}
			}
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} finally {

		}
	}

	/**
	 * 课程表查询
	 * 
	 * @param term
	 */
	private static void uimsCourse(int term) {
		// TODO Auto-generated method stub
		try {
			HttpPost post = new HttpPost("http://uims.jlu.edu.cn/ntms/service/res.do");
			String params = "{\"tag\":\"teachClassStud@schedule\",\"branch\":\"default\",\"params\":{\"termId\":" + term
					+ ",\"studId\":" + stuID + "}}";
			post.addHeader("Content-Type", "application/json");
			post.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
			post.setEntity(new StringEntity(params));

			HttpResponse response = client.execute(post);
			System.out.println("课表查询：" + response.getStatusLine());
			String result = EntityUtils.toString(response.getEntity());
			post.abort();

			JSONObject jsonObject = new JSONObject(result);
			JSONArray values = jsonObject.getJSONArray("value");
			for (int i = 0; i < values.length(); i++) {
				JSONObject value = values.getJSONObject(i);
				JSONObject teachClassMaster = value.getJSONObject("teachClassMaster");
				String tcsId = value.getString("tcsId");
				String maxStudCnt = teachClassMaster.getString("maxStudCnt");
				String studCnt = teachClassMaster.getString("studCnt");
				String courName = teachClassMaster.getJSONObject("lessonSegment").getString("fullName");

				JSONArray lessonSchedules = teachClassMaster.getJSONArray("lessonSchedules");
				LessonSchedule[] schedules = new LessonSchedule[lessonSchedules.length()];
				for (int j = 0; j < lessonSchedules.length(); j++) {// lessonSchedules
					JSONObject lessonSchedule = lessonSchedules.getJSONObject(j);
					schedules[j] = new LessonSchedule();
					if (lessonSchedule.has("classroom") && lessonSchedule.getJSONObject("classroom").has("fullName"))
						schedules[j].setClassroom(lessonSchedule.getJSONObject("classroom").getString("fullName"));
					else
						schedules[j].setClassroom("null");

					if (lessonSchedule.has("timeBlock") && lessonSchedule.getJSONObject("timeBlock").has("name"))
						schedules[j].setTime(lessonSchedule.getJSONObject("timeBlock").getString("name"));
					else
						schedules[j].setTime("null");

				}

				JSONArray lessonTeachers = teachClassMaster.getJSONArray("lessonTeachers");
				Teacher[] teachers = new Teacher[lessonTeachers.length()];
				for (int j = 0; j < lessonTeachers.length(); j++) {
					teachers[j] = new Teacher();
					teachers[j].setName(lessonTeachers.getJSONObject(j).getJSONObject("teacher").getString("name"));
				}
				System.out.println("科目:" + courName + "\t科目ID" + tcsId + "\t可容纳人数:" + maxStudCnt + "\t实际人数:" + studCnt);
				for (int j = 0; j < teachers.length; j++) {
					System.out.println("\t\t授课教师:" + teachers[j].getName());
				}
				for (int j = 0; j < schedules.length; j++) {
					System.out.println("\t\t教室:" + schedules[j].getClassroom() + "\t\t时间:" + schedules[j].getTime());
				}

				System.out.println();
			}

		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} finally {

		}

	}

	/***
	 * 课程查询
	 * 
	 * @param term
	 */
	private static void uimsjustCourse(int term) {
		// TODO Auto-generated method stub
		try {
			HttpPost post = new HttpPost("http://uims.jlu.edu.cn/ntms/service/res.do");
			String payLoad = "{\"tag\":\"termScore@inqueryTermScore\",\"branch\":\"default\",\"params\":{\"termId\":"
					+ term + ",\"studId\":" + stuID + "}}";
			post.addHeader("Content-Type", "application/json");
			post.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
			post.setEntity(new StringEntity(payLoad));

			HttpResponse response = client.execute(post);
			System.out.println("课程查询：" + response.getStatusLine());
			String result = EntityUtils.toString(response.getEntity());
			post.abort();

			JSONObject jsonObject = new JSONObject(result);
			JSONArray values = jsonObject.getJSONArray("value");
			for (int i = 0; i < values.length(); i++) {
				JSONObject value = values.getJSONObject(i);
				String courName = value.getJSONObject("lesson").getJSONObject("courseInfo").getString("courName");
				String termName = value.getJSONObject("teachingTerm").getString("termName");
				String isReselect = value.getJSONObject("apl").getString("isReselect");
				String credit = value.getJSONObject("apl").getJSONObject("planDetail").getString("credit");
				System.out.printf("学期：%2$s  学分：%4$-4s  重修：%3$s  课程：%1$-24s %n", courName, termName, isReselect, credit);
			}
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} finally {

		}
	}

	private static void uimsScoreCourse(int term) {
		// TODO Auto-generated method stub
		try {
			String payLoad = "{\"tag\":\"archiveScore@queryCourseScore\",\"branch\":\"byTerm\",\"params\":{\"studId\":"
					+ stuID + ",\"termId\":" + term + "},\"orderBy\":\"teachingTerm.termId, course.courName\"}";
			HttpPost scorebyCourse = new HttpPost("http://uims.jlu.edu.cn/ntms/service/res.do");
			scorebyCourse.addHeader("Content-Type", "application/json");
			scorebyCourse.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.89 Safari/537.36");
			scorebyCourse.setEntity(new StringEntity(payLoad));

			HttpResponse response = client.execute(scorebyCourse);
			System.out.println("成绩按学期查询：" + response.getStatusLine());
			String result = EntityUtils.toString(response.getEntity());
			scorebyCourse.abort();

			JSONObject scoreRaw = new JSONObject(result);
			JSONArray scoreValue = scoreRaw.getJSONArray("value");
			for (int i = 0; i < scoreValue.length(); i++) {
				JSONObject rawScore = scoreValue.getJSONObject(i);
				String courName = rawScore.getJSONObject("course").getString("courName");
				String asId = rawScore.getString("asId");// 双击显示比例
				String score = rawScore.getString("score");
				String scoreNum = rawScore.getString("scoreNum");
				String credit = rawScore.getString("credit");
				String isPass = rawScore.getString("isPass");
				String isReselect = rawScore.getString("isReselect");
				String gpoint = rawScore.getString("gpoint");
				String termName = rawScore.getJSONObject("teachingTerm").getString("termName");
				System.out.printf(
						"学期：%1$s  学分：%5$-3s  及格：%6$s 重修：%7$s 分数：%3$-4s(%4$s) 绩点：%8$-4s 内部ID:%9$s   课程：%2$-24s%n",
						termName, courName, score, scoreNum, credit, isPass, isReselect, gpoint, asId);
			}

		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} finally {

		}
	}

	private static void uimsScoreRate(int asId) {
		// TODO Auto-generated method stub
		// 比例查询
		try {
			HttpPost scoreRate = new HttpPost("http://uims.jlu.edu.cn/ntms/score/course-score-stat.do");
			String params = "{\"asId\":\"" + asId + "\"}";
			scoreRate.addHeader("Content-Type", "application/json");
			scoreRate.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.89 Safari/537.36");
			scoreRate.setEntity(new StringEntity(params));
			HttpResponse response = client.execute(scoreRate);
			if (response.getStatusLine().getStatusCode() == 200) {
				String resultRate = EntityUtils.toString(response.getEntity());
				JSONObject rate = new JSONObject(resultRate);
				JSONArray items = rate.getJSONArray("items");
				System.out.println();
				for (int j = 0; j < items.length(); j++) {
					JSONObject item = items.getJSONObject(j);
					String label = item.getString("label");
					label = String.format("%-15s", label);
					String percent = item.getString("percent");
					System.out.println("\t\t等级：" + label + "占比：" + percent);
				}
				System.out.println("\n");
			}
			scoreRate.abort();
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}finally {
			
		}
	}

	/**
	 * 退出登录
	 */
	private static void uimsLogout() {
		// TODO Auto-generated method stub
		try {
			HttpGet get = new HttpGet("http://uims.jlu.edu.cn/ntms/logout.do");
			get.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
			HttpResponse response = client.execute(get);
			System.out.println("退出UIMS(步骤一)\t" + response.getStatusLine());
			get.abort();

			HttpGet get1 = new HttpGet("http://uims.jlu.edu.cn/ntms/j_spring_security_logout");
			get1.addHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
			response = client.execute(get1);
			System.out.println("退出UIMS(步骤二)\t" + response.getStatusLine());
			get1.abort();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				client.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}

}

class LessonSchedule {
	private String classroom = null;
	private String time = null;

	public String getClassroom() {
		return classroom;
	}

	public void setClassroom(String classroom) {
		this.classroom = classroom;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

}

class Teacher {
	private String name = null;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
