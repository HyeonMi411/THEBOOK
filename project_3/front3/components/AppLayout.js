// components/AppLayout.js   # 재사용 가능한 UI 컴포넌트 폴더 
//1. require
import { Layout, Menu, Input, Row, Col, Drawer, Button, Grid } from "antd";  
import { MenuOutlined, SearchOutlined } from "@ant-design/icons";   
import { useSelector, useDispatch}  from 'react-redux'; // 전역상태 , 액션스토어알림
import { useRouter }                from 'next/router'; // 경로이동
import { useEffect,  useState   }  from 'react';       // 이벤트변경감지, 변수
import  Link                        from 'next/link';
import  BookSearchBox               from './BookSearchBox';   // ★boot1 헤더 AJAX 검색창
import  Footer                      from './Footer';           // ★boot1 푸터

const  {Header, Content} = Layout;    // <Layout.Header> → <Header>  
const  {useBreakpoint} = Grid;
import {logoutRequest , loginSuccess }  from '../reducers/authReducer';  //##

import  axios from "axios";

//2. 부품
// Header / Drawer
function AppLayout({  children , initialUser  }){   //★ 대체부품, 초기값
    // 변수, 셋팅함수
    const [ drawerOpen , setDrawerOpen ] = useState(false);  
    const router       = useRouter();
    const dispatch     = useDispatch();
    const {user}       = useSelector((state)=> state.auth);
    
 
    
    const handleLogout = ()=>{   dispatch(logoutRequest());   router.replace('/login');  };  // 디스패치(logoutRequest()) / 경로 login 넘기기   //##




    const menuItems = [
       ...( user  &&  user.nickname
        ? [
            { key: "books",     label: <Link href="/books">📚 BOOK</Link> },       // ★도서 (전체공개)
            { key: "notices",   label: <Link href="/notices">📢 NOTICE</Link> },   // ★공지사항 (전체공개)
            ...(user.role === "ROLE_ADMIN"
              ? [
                  { key: "newBook",   label: <Link href="/books/new">📘 도서등록</Link> },     // ★관리자전용
                  { key: "newNotice", label: <Link href="/notices/new">📝 공지작성</Link> },   // ★관리자전용
                ]
              : []),            
            { key: "profile",   label: <Link href="/mypage">👤 MYPAGE </Link> },
            { key: "logout",    label: <a onClick={handleLogout}  style={{cursor:"pointer"}} >🔓 로그아웃</a> },
        ]
        : [ 
            { key: "books",     label: <Link href="/books">📚 BOOK</Link> },       // ★비로그인도 조회는 가능
            { key: "notices",   label: <Link href="/notices">📢 NOTICE</Link> },   // ★비로그인도 조회는 가능
            { key: "login",     label: <Link href="/login">🔒 Login</Link> },
            { key: "signup",    label: <Link href="/signup">👤 Signup</Link> },
        ]
      ) 
    ]; 
    ////////////#1) Row (줄) - Col(칸)   /  Col
    ////////////#2) 반응형속성 (모바일 : xs, sm, 태블릿: md, pc: lg) - 24칸 
    //  display:"flex"  자식요소 배치 알아서
    //  justify="space-between"  양쪽에 콘텐츠 배치 
    return   (<Layout className="bookstore-body">
    {/* Header - ★boot1(BookStore) 로고 + 검색창을 antd Header 안에 통합 */}
    <Header  style={{display:"flex"}}>  
        <Row align="middle" justify="space-between"  style={{width:"100%"}} gutter={16}>
            <Col  flex="none">
                <Link href="/">    
                    <a className="bs-logo" style={{fontSize:"20px", color:"#8ab4f8"}}>
                        📚 BookStore
                    </a>
                </Link>
            </Col>
            {/* 검색창 - 태블릿 이상에서만 노출 (모바일은 Drawer 메뉴로) */}
            <Col flex="auto" xs={0} sm={0} md={8} lg={9} style={{maxWidth:400}}>
                <BookSearchBox />
            </Col>
            {/*  xs, sm (모바일): 0 숨김처리  ,  md (테블릿) : 16  24칸중에 16 , lg(pc) : 18 */}
            <Col flex="auto" xs={0}  sm={0}  md={16}  lg={18}>
                <Menu
                theme="dark"
                mode="horizontal" 
                items={menuItems} 
                />
            </Col>
            {/*  button 종류 : primary , default(하얀색), text(없음) , link(a링크형식모양)  */}
            <Col  flex="none"  xs={2}   md={0}>
                <Button 
                type="text" 
                icon={ <MenuOutlined  style={{color:"white" , fontSize:20 }} />}
                onClick={()=>setDrawerOpen(true)}> 
                </Button>
            </Col>
        </Row>
    </Header> 
    <Drawer
    title="MENU"
    placement="right" 
    onClose={()=> setDrawerOpen(false)}
    open={drawerOpen}
    >
        {/* 모바일에서는 Drawer 안에 검색창도 함께 노출 */}
        <div style={{marginBottom:16}}>
            <BookSearchBox />
        </div>
        <Menu 
        mode="vertical" 
        items={menuItems}  
        />
    </Drawer>
    <Content  style={{ padding: "40px" }}>{children}</Content>
    {/* Footer - ★boot1(BookStore) 푸터 */}
    <Footer />
    </Layout>);
}
//3. export
export   default  AppLayout;

// Layout: https://ant.design/components/layout 
// Menu: https://ant.design/components/menu 
// Input: https://ant.design/components/input 
// Drawer: https://ant.design/components/drawer 
// Grid(Row/Col): https://ant.design/components/grid 
// Button: https://ant.design/components/button