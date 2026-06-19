<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>   

<%@include file="../inc/header_footer.jsp"  %>
<!-- 	header		 -->
<!-- 	header		 -->
<script>
window.addEventListener("load" , function(){
	let  result = '${result}';   // el
	console.log(result);
	
	if( result == "글쓰기 실패" || result == "비밀번호 확인!"){  alert(result);  history.go(-1);  }  // 알림창, 뒤로 가기
	else if(result.length != 0){  alert(result);                   }  
}); 
</script>


    <!--  content -->
    <section class="container  my-5">
        <h3> MultiBoard </h3>
<%--         <pre>
        페이징 :  ${paging}
        전체리스트 : ${list}
        </pre> --%>
        <table  class="table  table-striped  table-bordered table-hover">
            <caption> BOOK 목록 </caption>
            <thead>
                <tr>
                    <th scope="col">NO</th>
                    <th scope="col">TITLE</th>
                    <th scope="col">WRITER</th>
                    <th scope="col">DATE</th>
                    <th scope="col">HIT</th>
                </tr>
            </thead>
            <tbody>
              
			 
			
            </tbody>
            <tfoot><tr><td colspan="5">
             
            </ul></td></tr>
            </tfoot>
        </table>

        <div  class="text-end">
           <a href="${pageContext.request.contextPath}/book/insert"  
           	  title="글쓰기 폼"  class="btn btn-primary" >글쓰기</a>
        </div>

    </section>
