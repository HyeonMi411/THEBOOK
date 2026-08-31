//__tests__/postReducer.test.js
import postReducer, {  
    fetchPostsRequest , fetchPostsSuccess, fetchPostsFailure ,   // 전체글
    fetchPostDetailRequest  , fetchPostDetailSuccess  , fetchPostDetailFailure,  //상세글 
    createPostRequest , createPostSuccess , createPostFailure ,  // 글쓰기
    updatePostRequest ,  updatePostSuccess ,  updatePostFailure ,  // 글수정
    deletePostRequest ,  deletePostSuccess ,  deletePostFailure ,  // 글삭제
    resetPostState // 초기화  ( postReducer 에는 resetPostState 가 있고 resetUserState 는 없음 )
} from '../postReducer';

describe('post' , ()=>{ 
    const initialState={
        posts : [] ,       // 전체게시글 목록
        currentPost: null, // 단건 조회된 상세 게시글
        loading: false,
        error: null,
        success : false,
    };

    it(  'fetchPostsRequest & fetchPostsSuccess' , ()=>{    // fetchPostsRequest , fetchPostsSuccess
        let state = postReducer(  initialState , fetchPostsRequest() );
        //1. fetchPostsRequest() 실행 - 인자 없음
        //2. 리듀서툴킷 - { type:fetchPostsRequest  , payload : undefined } 객체만들기
        //3. 리듀서 fetchPostsRequest:(state , action)=>{} 액션받아서 처리
        // action = { type:fetchPostsRequest  , payload : undefined }
        expect(state.loading).toBe(true);

        const  posts = [ {id:1, content:'첫 번째 글'} ];
        state = postReducer(  initialState , fetchPostsSuccess(posts) );
        expect(state.loading).toBe(false);
        expect(state.posts).toEqual(posts);
        // postReducer.js 의 fetchPostsSuccess 는 loading/posts 만 갱신하고
        //  success 플래그는 건드리지 않습니다(success 는 글쓰기 성공시에만 true 로 씀).
        expect(state.success).toBe(false);
    });

    // 테스트파일
    // fetchPostDetailSuccess
    it(  'fetchPostDetailSuccess' , ()=>{    // fetchPostDetailSuccess
        const post  = {id:1, content:'첫 번째 글'};
        const state = postReducer(initialState ,  fetchPostDetailSuccess(post)); 
        expect(state.currentPost).toEqual(post);
        expect(state.loading).toBe(false);
    });
    // createPostSuccess   - 글쓰기
    it(  'createPostSuccess' , ()=>{    // fetchPostDetailSuccess
        const newPost  = {id:3, content:'새 글'};
        const state = postReducer(initialState ,  createPostSuccess(newPost)); 
        expect(state.posts[0]).toEqual(newPost);
        expect(state.success).toBe(true);
    });
    // createPostSuccess: (state , action)=>{ 
    //    state.loading = false;
    //    state.posts   = [action.payload,   ...state.posts];  // 새글을 목록상단추가
    //    state.success = true;
    // },    
    
    // updatePostSuccess   - 글수정
    it(  'updatePostSuccess' , ()=>{    // fetchPostDetailSuccess
        const prev    =  {   ...initialState   , posts : [{id:3, content:'새 글'}]  }; 
        const updated =  {id:3, content:'수정 후'};  //서버에서 받아온값
        
        const state   =  postReducer( prev, updatePostSuccess(updated) );
        expect(state.posts[0].content).toBe('수정 후');
        expect(state.currentPost).toEqual(  updated );
    });
    // updatePostSuccess: (state , action)=>{ 
    //    state.loading = false;
    //    state.posts   = state.posts.map(  post => 
    //        post.id === action.payload.id ? action.payload : post
    //    );   
    //    state.currentPost = action.payload;
    //    state.success = true;
    // }, 

    //  deletePostSuccess  - 글삭제
    it(  'deletePostSuccess' , ()=>{    // fetchPostDetailSuccess
        const prev    =  {   ...initialState   , posts : [{id:1, content:'새 글'}]  };  
        const state   =  postReducer( prev, deletePostSuccess(1) );
        expect(state.posts).toHaveLength(0);
        expect(state.posts.length).toBe(0);
        // postReducer.js 의 deletePostSuccess 는 loading/posts 만 갱신하고 success 는 건드리지 않음
        expect(state.success).toBe(false);
    });
    // resetPostState  -  초기화
    it(  'resetPostState' , ()=>{    // fetchPostDetailSuccess
        const prev    =  {   ...initialState   , loading:true, error:'error' , success:true  };  
        const state   =  postReducer( prev, resetPostState() ); 
        expect(state.loading).toBe(false);
        expect(state.error).toBeNull();
        expect(state.success).toBe(false);
    });  
});  
        
// npm test postReducer

// deletePostSuccess: (state , action)=>{ 
//    state.loading = false;
//    // 삭제된 게시글의 id받아서 목록에서 제외
//    state.posts   = state.posts.filter(post=>  post.id !== action.payload);   
//    state.success = true;
// },