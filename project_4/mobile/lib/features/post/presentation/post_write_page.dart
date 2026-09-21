
import 'dart:typed_data';
import 'package:flutter/foundation.dart';   // 기본유틸
import 'package:flutter/material.dart'; // ui 컴포넌트 모음
import 'package:flutter_riverpod/flutter_riverpod.dart';  // riverpod : 전역상태관리
import 'package:image_picker/image_picker.dart';  // 이미지 선택
import '../data/board_provider.dart'; // 전역상태 + 서버연동데이터가져오는기능   (boardProvider)
import '../../auth/data/auth_provider.dart';  // 인증상태 프로바이더

class PostWritePage extends ConsumerStatefulWidget {  // ConsumerStatefulWidget
  const PostWritePage({super.key});

  @override
  ConsumerState<PostWritePage> createState() => _PostWritePageState();
}

class _PostWritePageState extends ConsumerState<PostWritePage> {
  final _contentController = TextEditingController();   // 입력컨트롤러
  final _hashtagController = TextEditingController();
  
  final ImagePicker _picker = ImagePicker();  // 이미지선택
  List<XFile> _selectedImages = [];
  
  final Map<String, Uint8List> _imageBytesCache = {};

  @override
  void dispose() {  // 위젯메모리해제
    _contentController.dispose();
    _hashtagController.dispose();
    super.dispose();
  }

  Future<void> _pickImages() async {
    final List<XFile> images = await _picker.pickMultiImage();  //다중이미지 선택  - pickMultiImage
    if (images.isNotEmpty) {
      for (var image in images) { // 선택된파일들
        final bytes = await image.readAsBytes();  // 바이트 데이터 추출
        _imageBytesCache[image.path] = bytes; // 경로를 바이트 저장
      }
      setState(() { // ui 그리기
        _selectedImages = images;
      });
    }
  }

  void _handleSubmit() async {
    final content = _contentController.text.trim();   // 본문텍스트 공백제거
    final hashtags = _hashtagController.text.trim();
    
    // 유저 ID 안전 추출
    final user = ref.read(authProvider).user; // redux+sage  = provider 기능의 user 가져오기
    final rawId = user?['id'] ?? user?['userId'] ?? user?['memberId'] ?? '1';   // 백엔드 필드 대응 null-aware 키 추출
    final userId = rawId.toString();

    if (content.isEmpty) {  // 빈칸
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('내용을 입력해주세요.')),
      );
      return;
    }
    // react :  redux+sage  = provider  boot에요청 
    final success = await ref.read(boardProvider.notifier).createPost(
      userId: userId,
      content: content,
      hashtags: hashtags,
      imageFiles: _selectedImages,
    );

    if (success && mounted) {
      // 등록 성공 알림 띄우기 (화면 상단/하단 SnackBar)
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('게시글이 성공적으로 등록되었습니다! 🎉'),
          duration: Duration(seconds: 2),
          behavior: SnackBarBehavior.floating, // 바닥에 붙지 않고 떠있는 깔끔한 스타일
        ),
      );
      Navigator.pop(context);   // 현대화면 닫고 전화면 이동 
    }
  }
  ///////////////////////////////////////////////////////////////////////////////////////////
  @override
  Widget build(BuildContext context) {
    return Scaffold(  // 앱의 기본골격
      appBar: AppBar(title: const Text('새 글 작성')),  // 상단앱바
      body: Padding(  // 부품 Padding: 여백레이아웃
        padding: const EdgeInsets.all(16.0),  // 4개 여백
        child: ListView(  // 부품 ListView: 스크롤가능한 리스트뷰
          children: [
            TextField(  // 부품 TextField:  입력폼 위젯
              controller: _contentController, // 컨트롤러 바인딩
              decoration: const InputDecoration(labelText: '내용 입력'),    //라벨
              maxLines: 5,  // 줄공간확보
            ),
            const SizedBox(height: 12), // 세로사이즈 12px 
            TextField(
              controller: _hashtagController,
              decoration: const InputDecoration(labelText: '해시태그 (예: #flutter, #spring)'),
            ),
            const SizedBox(height: 20),
            
            ElevatedButton.icon(  // 부품 ElevatedButton:  아이콘포함 버튼
              onPressed: _pickImages,   // on 시작 이벤트 연결
              icon: const Icon(Icons.image),
              label: Text('이미지 첨부하기 (${_selectedImages.length}장 선택됨)'),
            ),
            const SizedBox(height: 12),

            if (_selectedImages.isNotEmpty)
              SizedBox(
                height: 100,  // 프리뷰 영역 100px 제한
                child: ListView.builder(  // 동적리스트 생성
                  scrollDirection: Axis.horizontal, // 스크롤바 가로
                  itemCount: _selectedImages.length,  // 선택갯수
                  itemBuilder: (context, index) {
                    final image = _selectedImages[index];
                    final bytes = _imageBytesCache[image.path];

                    return Padding(
                      padding: const EdgeInsets.only(right: 8.0),   // 오르쪽여백
                      child: Stack( // 부품: 위젯겹치는 배치레이아웃
                        children: [
                          bytes != null
                              ? Image.memory(bytes, width: 100, height: 100, fit: BoxFit.cover)
                              : Container(width: 100, height: 100, color: Colors.grey[300]),
                          Positioned(
                            top: 0,
                            right: 0,
                            child: IconButton(
                              icon: const Icon(Icons.remove_circle, color: Colors.red),
                              onPressed: () {
                                setState(() {
                                  _imageBytesCache.remove(image.path);  // 캐시제거
                                  _selectedImages.removeAt(index);
                                });
                              },
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),

            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,   // 가로 너비 100%
              child: ElevatedButton(   // 등록 실행버튼
                style: ElevatedButton.styleFrom(backgroundColor: Colors.blue, foregroundColor: Colors.white),
                onPressed: _handleSubmit,
                child: const Text('등록하기', style: TextStyle(fontSize: 16)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
