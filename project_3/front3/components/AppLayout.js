// components/AppLayout.js   # 재사용 가능한 UI 컴포넌트 폴더
// 1. require
import { Layout, Menu, Row, Col, Drawer, Button } from "antd";
import { MenuOutlined } from "@ant-design/icons";
import { useSelector, useDispatch } from 'react-redux'; // 전역상태 , 액션 스토어 알림
import { useRouter } from 'next/router';                 // 경로이동
import { useState } from 'react';                        // 이벤트변경감지, 변수
import Link from 'next/link';
import BookSearchBox from './BookSearchBox';              // boot1 헤더 AJAX 검색창
import Footer from './Footer';                            // boot1 푸터

const { Header, Content } = Layout;    // <Layout.Header> → <Header>
import { logoutRequest, loadUserRequest } from '../reducers/authReducer';
import { fetchCartRequest } from '../reducers/cartReducer';
import { useEffect } from 'react';

// 2. 부품
// Header / Drawer
function AppLayout({ children }) {
    // 변수, 셋팅함수
    const [drawerOpen, setDrawerOpen] = useState(false);
    const router   = useRouter();
    const dispatch = useDispatch();
    const { user } = useSelector((state) => state.auth);
    const { items: cartItems } = useSelector((state) => state.cart); // 장바구니 담긴 개수 뱃지용

    // 새로고침, 혹은 카카오페이 결제창(외부 도메인)에서 우리 사이트로 돌아오는 것처럼
    // 브라우저가 완전히 새로 페이지를 로드하면 Redux 스토어가 처음부터 다시 만들어져서
    // user 가 null 이 됨. localStorage 에 accessToken 이 남아있다면(진짜 로그아웃한
    // 게 아니라면) 그 토큰으로 내 정보를 다시 불러와서 로그인 상태를 복원.
    // 이게 없으면, 결제 완료 후 "주문내역 보기"처럼 로그인이 필요한 화면으로 이동할 때
    // 실제로는 로그인되어 있는데도 user 가 비어있어서 로그인 화면으로 튕겨나가 버림.
    useEffect(() => {
        if (!user && typeof window !== 'undefined' && localStorage.getItem('accessToken')) {
            dispatch(loadUserRequest());
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (user && user.nickname) dispatch(fetchCartRequest());
    }, [user, dispatch]);

    // 로그아웃 버튼 클릭시, 여기서 바로 /login 으로 이동시키면 안 됨.
    // dispatch(logoutRequest())는 saga 를 비동기로 실행시키는데(백엔드 로그아웃 API
    // 호출 → 소셜 provider 확인 → 카카오/네이버면 그 사이트로 실제 이동), 여기서 곧바로
    // router.replace('/login') 을 하면 saga 가 이동시키기도 전에 사용자를 먼저 보내버려서
    // 소셜 로그아웃이 실행 안 되는 것처럼 보이게 됨. 최종 이동은 saga(authSaga.js
    // 의 logout())가 책임지므로, 여기서는 액션만 dispatch .
    const handleLogout = () => {
        dispatch(logoutRequest());
    };

    // antd Menu 의 items.label 안에 <a onClick={...}> 를 직접 넣는 방식 대신,
    // Menu 자체의 onClick 콜백(공식 권장 패턴)을 씁니다. label 내부에 onClick을
    // 직접 넣는 방식은 antd 버전/설정에 따라 Menu 내부 이벤트 처리와 충돌해서
    // 클릭이 씹히는 경우가 보고되어 있어, 더 안정적인 방식으로 바꿨음.
    const handleMenuClick = ({ key }) => {
        if (key === 'logout') {
            handleLogout();
        }
    };

    const menuItems = [
        ...(user && user.nickname
            ? [
                { key: "books",   label: <Link href="/books">📚 BOOK</Link> },     // 도서 (전체공개)
                { key: "notices", label: <Link href="/notices">📢 NOTICE</Link> }, // 공지사항 (전체공개)
                { key: "nl",      label: <Link href="/books/national-library">🏛 국립중앙도서관</Link> }, // 전체공개, 저장만 관리자전용
                ...(user.role === "ROLE_ADMIN"
                    ? [

                    ]
                    : []),
                { key: "cart",    label: <Link href="/cart">{`🛒 CART${cartItems?.length > 0 ? ` (${cartItems.length})` : ''}`}</Link> }, // 장바구니
                { key: "orders",  label: <Link href="/mypage/orders">📋 주문내역</Link> }, // 내 주문내역                
                { key: "ai-chat", label: <Link href="/ai/chat">🤖 AI 챗봇</Link> }, // PDF 문서 기반 질의응답
                { key: "profile", label: <Link href="/mypage">👤 MYPAGE </Link> },
                { key: "logout",  label: <span style={{ cursor: "pointer" }}>🔓 로그아웃</span> }, // 클릭은 Menu의 onClick(handleMenuClick)이 처리
            ]
            : [
                { key: "books",   label: <Link href="/books">📚 BOOK</Link> },     // 비로그인도 조회는 가능
                { key: "notices", label: <Link href="/notices">📢 NOTICE</Link> }, // 비로그인도 조회는 가능
                { key: "nl",      label: <Link href="/books/national-library">🏛 국립중앙도서관</Link> }, // 비로그인도 검색은 가능
                { key: "login",   label: <Link href="/login">🔒 Login</Link> },
                { key: "signup",  label: <Link href="/signup">👤 Signup</Link> },
            ]
        ),
    ];

    // Row(줄) - Col(칸) 구조, 반응형(모바일: xs, sm, 태블릿: md, pc: lg) - 24칸
    // display:"flex"  자식요소 배치 알아서
    // justify="space-between"  양쪽에 콘텐츠 배치
    return (
        <Layout className="bookstore-body">
            {/* Header - boot1(BookStore) 로고 + 검색창을 antd Header 안에 통합 */}
            {/* overflow: hidden - wrap={false} 로 줄바꿈은 막았지만, 혹시라도 내용이 Header의
                고정 높이를 넘어서 삐져나오면서 겹쳐 보이는 것까지 이중으로 방지 */}
            <Header style={{ display: "flex", overflow: "hidden" }}>
                {/* wrap={false} - Row 는 기본적으로 flex-wrap:wrap 이라, 로고+검색창+가로메뉴를
                    한 줄에 담을 폭이 부족해지면 자동으로 다음 줄로 줄바꿈되어 헤더가 2줄(이중)로
                    보이는 문제가 있었음. 태블릿 폭(md, 768px)에서 정확히 이 문제가 발생.
                    wrap={false} 로 항상 한 줄을 유지하도록 강제. */}
                <Row align="middle" justify="space-between" wrap={false} style={{ width: "100%" }} gutter={16}>
                    <Col flex="none">
                        {/* 로고 클릭시 도서 목록(/books)으로 이동 */}
                        <Link href="/books">
                            <a className="bs-logo" style={{ fontSize: "20px", color: "#8ab4f8" }}>
                                📚 BookStore
                            </a>
                        </Link>
                    </Col>

                    {/* 검색창/가로메뉴 - 태블릿(md=768px)까지는 항목이 많아 한 줄에 다 안 들어가서
                        헤더가 줄바꿈(이중화)되는 문제가 있었음. lg(992px) 이상에서만 노출하도록
                        변경해서, 태블릿 폭에서는 계속 햄버거(Drawer) 메뉴를 쓰도록 처리. */}
                    <Col flex="auto" xs={0} sm={0} md={0} lg={9} style={{ maxWidth: 400 }}>
                        <BookSearchBox />
                    </Col>

                    {/*  xs, sm, md(태블릿까지): 0 숨김처리  ,  lg(PC) 부터만 가로메뉴 노출 */}
                    <Col flex="auto" xs={0} sm={0} md={0} lg={18}>
                        <Menu theme="dark" mode="horizontal" items={menuItems} onClick={handleMenuClick} />
                    </Col>

                    {/*  button 종류 : primary , default(하얀색), text(없음) , link(a링크형식모양)  */}
                    {/*  lg(992px) 미만에서는 계속 햄버거 버튼 노출 (태블릿 포함) */}
                    <Col flex="none" xs={2} lg={0}>
                        <Button
                            type="text"
                            icon={<MenuOutlined style={{ color: "white", fontSize: 20 }} />}
                            onClick={() => setDrawerOpen(true)}
                        />
                    </Col>
                </Row>
            </Header>

            <Drawer
                title="MENU"
                placement="right"
                onClose={() => setDrawerOpen(false)}
                open={drawerOpen}
            >
                {/* 모바일에서는 Drawer 안에 검색창도 함께 노출 */}
                <div style={{ marginBottom: 16 }}>
                    <BookSearchBox />
                </div>
                <Menu mode="vertical" items={menuItems} onClick={handleMenuClick} />
            </Drawer>

            <Content style={{ padding: "40px" }}>{children}</Content>

            {/* Footer - boot1(BookStore) 푸터 */}
            <Footer />
        </Layout>
    );
}

// 3. export
export default AppLayout;

// Layout: https://ant.design/components/layout
// Menu: https://ant.design/components/menu
// Drawer: https://ant.design/components/drawer
// Grid(Row/Col): https://ant.design/components/grid
// Button: https://ant.design/components/button
