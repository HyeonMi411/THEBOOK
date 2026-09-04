//1. require / import
import { Row, Col, Form, Input, Button, Upload, Spin, message } from "antd";    
import { UploadOutlined } from "@ant-design/icons";   
// store : useSelector(전역)      , useDispatch(스토어이벤트알림)  
// 감지 : useEffect(이벤트변경감지) , useState( 변수 ) 
// 경로 : useRouter
import React , {useState , useEffect , useRef}  from  "react";
import {useSelector , useDispatch}  from  "react-redux";
import {useRouter} from "next/router";
import { signupRequest , resetUserState } from "../reducers/authReducer";

import  api  from  "../api/axios";   

//2. function (부품)
function   SignupPage(){
    //5개부품  -  useEffect(이벤트변경감지) , useState( 변수 ) 
    const dispatch = useDispatch();  //이벤트변경감지
    const router   = useRouter();    // 경로
    const { user, error, success, loading  } = useSelector((state) => state.auth);   
    
    const [fileList, setFileList] = useState([]);
    const  isSubmittedRef         = useRef(false);  

    // 이메일 인증 상태 - 인증완료 후에도 이메일을 다시 바꾸면 재인증하도록 어떤
    // 이메일로 인증을 완료했는지(verifiedEmail)까지 함께 기억
    const [emailCode, setEmailCode]         = useState("");
    const [codeSent, setCodeSent]           = useState(false);
    const [verifiedEmail, setVerifiedEmail] = useState("");
    const [emailSending, setEmailSending]   = useState(false);
    const [codeVerifying, setCodeVerifying] = useState(false);
    const formRef = React.useRef(null);

    const sendEmailCode = async () => {
        const email = formRef.current?.getFieldValue("email");
        if (!email) { message.warning("이메일을 먼저 입력하세요."); return; }
        setEmailSending(true);
        try {
            await api.post(`/auth/email/send-code?email=${encodeURIComponent(email)}`);
            setCodeSent(true);
            message.success("인증번호를 발송했습니다. 메일함을 확인하세요.");
        } catch (err) {
            message.error("인증번호 발송에 실패했습니다.");
        } finally {
            setEmailSending(false);
        }
    };

    const verifyEmailCode = async () => {
        const email = formRef.current?.getFieldValue("email");
        if (!emailCode) { message.warning("인증번호를 입력하세요."); return; }
        setCodeVerifying(true);
        try {
            await api.post(`/auth/email/verify-code?email=${encodeURIComponent(email)}&code=${encodeURIComponent(emailCode)}`);
            setVerifiedEmail(email);
            message.success("이메일 인증이 완료되었습니다.");
        } catch (err) {
            message.error("인증번호가 일치하지 않거나 만료되었습니다.");
        } finally {
            setCodeVerifying(false);
        }
    };

    // 데이터 받아서 회원가입전송  - 네트워크가 느리면 0.5초 2~3회 연속으로 클릭 (회원가입요청중복)
    const onFinish  = ( values )=>{
        if( isSubmittedRef.current ) return;  
        isSubmittedRef.current = true;  

        const formData = new FormData();
        formData.append("email" ,values.email );
        formData.append("password" ,values.password );
        formData.append("nickname" ,values.nickname );
        if(fileList.length > 0 ){   formData.append("ufile" ,fileList[0].originFileObj);  } 
        dispatch( signupRequest(formData) );    
    };
    useEffect(()=> {
        if(success){
            message.success("회원가입이 성공적으로 완료되었습니다.");
            router.push(`/login`);
            //router.push(`/`);   
            dispatch( resetUserState() );   
        }
        return ()=>{
          isSubmittedRef.current = false;
        };

    } , [success, router , dispatch]);

    /////////////////////// Layout > Row >  Col  Col
    // 모바일제일작은사이즈 : 24 xm={}   모바일2: 16  sm={}  태블릿:8  md={} / lg={}
    return (<Row  justify="center">
        <Col xm={24}  sm={16}  md={8}   >
        {  loading  && <Spin/>  }
        {  error    && <p  style={{color:"red"}}> {error} </p> }
        {  !success && ( 
        <Form ref={formRef} layout="vertical" onFinish={onFinish}>
          {/* 이메일입력 + 중복검사 Form.Item    >  Input  /  name , hasFeedback 아이콘  */}
          <Form.Item
            label="이메일"
            name="email"
            hasFeedback
            rules={[ 
              { required: true , message: '이메일을 입력하세요.'} , 
              { validator: async( _ , value)=>{   
                    if(!value)  return  Promise.resolve();  // 값 없어서 그냥 바로반환

                    try{ // boot에 시도
                      const res = await  api.get(`/auth/check-email?email=${encodeURIComponent(value)}`);
                      if(res?.data === true){
                        return  Promise.reject(new Error("이미 사용중인 이메일입니다."));    //오류 바로 반환
                      }
                      return  Promise.resolve();   //성공했으니깐 바로 반환
                    }catch(err){
                       return  Promise.reject(new Error("중복검사 실패"));   //오류 바로 반환
                    }

                }
              },
             ]}
          >  
            <Input/>
          </Form.Item>

          {/* 이메일 인증번호 발송/확인 - 회원가입은 이 인증을 완료해야 서버에서 허용 */}
          <Row gutter={8} style={{ marginBottom: 8 }}>
            <Col flex="auto">
              <Input
                placeholder="인증번호 6자리"
                value={emailCode}
                onChange={(e) => setEmailCode(e.target.value)}
                disabled={!codeSent || !!verifiedEmail}
                maxLength={6}
              />
            </Col>
            <Col>
              <Button onClick={sendEmailCode} loading={emailSending} disabled={!!verifiedEmail}>
                인증번호 받기
              </Button>
            </Col>
            <Col>
              <Button onClick={verifyEmailCode} loading={codeVerifying} disabled={!codeSent || !!verifiedEmail}>
                확인
              </Button>
            </Col>
          </Row>
          {verifiedEmail && <p style={{ color: "green" }}>이메일 인증 완료 ({verifiedEmail})</p>}

         {/* 비밀번호 입력 */}
          <Form.Item
            label="비밀번호"
            name="password"
            rules={[ {required: true , message: '비밀번호를 입력하세요.'} ]}
          >  
            <Input.Password/>
          </Form.Item>
          
          {/* 닉네임 입력 + 중복검사  */}
          <Form.Item
            label="닉네임"
            name="nickname"
            hasFeedback
            rules={[ {required: true , message: '닉네임을 입력하세요.'}, 
              { validator: async( _ , value)=>{   
                    if(!value)  return  Promise.resolve();  // 값 없어서 그냥 바로반환

                    try{ // boot에 시도
                      const res = await  api.get(`/auth/check-nickname?nickname=${encodeURIComponent(value)}`);
                      if(res?.data === true){
                        return  Promise.reject(new Error("이미 사용중인 닉네임입니다."));    //오류 바로 반환
                      }
                      return  Promise.resolve();   //성공했으니깐 바로 반환
                    }catch(err){
                       return  Promise.reject(new Error("중복검사 실패"));   //오류 바로 반환
                    }

                }
              },]}
          >  
            <Input/>
          </Form.Item>

          {/* 프로필 이미지 업로드 */}    
          <Form.Item  name="profileImage"  label="프로필 이미지">
            <Upload
              beforeUpload={()=>false}
              fileList={  fileList   }
              onChange={ ( {fileList} )=> setFileList(fileList)  }
              maxCount={1}
            >
                <Button  icon={ <UploadOutlined/> }>이미지 선택</Button>
            </Upload>
          </Form.Item>    


          <Button  type="primary"  htmlType="submit" disabled={!verifiedEmail} >회원가입</Button>
          </Form>
        )}  
        </Col>
    </Row>);
}
//3. export
export default SignupPage;


///// ver-0
// export default function SignupPage(){
//   return "SIGNUP";
// }