import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';

/// 앱에 포함된 기본 이미지 (assets/images/avatars) - 앨범에 사진이 없어도 선택 가능
const List<String> defaultImageAssets = [
  'assets/images/avatars/one.png',
  'assets/images/avatars/two.png',
  'assets/images/avatars/three.png',
  'assets/images/avatars/four.png',
  'assets/images/avatars/five.png',
  'assets/images/avatars/six.png',
  'assets/images/avatars/seven.png',
];

/// 이미지 고르기 공통 시트 - [기본 이미지 7개] + [앨범에서 선택] (+ 선택적으로 [카메라])
///
/// 반환값은 image_picker 와 같은 XFile 이라서, 기존 업로드 코드(readAsBytes / name)를 그대로 쓸 수 있다.
/// 기본 이미지를 고르면 에셋 바이트로 XFile 을 만들어 돌려준다 → 서버에는 일반 PNG 업로드와 똑같이 전송됨.
Future<XFile?> pickImageWithDefaults(
  BuildContext context, {
  String title = '이미지 선택',
  bool allowCamera = false,
  double maxWidth = 1200,
}) async {
  final Object? choice = await showModalBottomSheet<Object>(
    context: context,
    showDragHandle: true,
    builder: (ctx) => SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          const Text('기본 이미지', style: TextStyle(color: Colors.grey)),
          const SizedBox(height: 8),
          GridView.count(
            crossAxisCount: 4,
            shrinkWrap: true,
            mainAxisSpacing: 10,
            crossAxisSpacing: 10,
            physics: const NeverScrollableScrollPhysics(),
            children: [
              for (final String asset in defaultImageAssets)
                InkWell(
                  borderRadius: BorderRadius.circular(40),
                  onTap: () => Navigator.pop(ctx, asset),
                  child: ClipOval(child: Image.asset(asset, fit: BoxFit.cover)),
                ),
            ],
          ),
          const Divider(height: 28),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.photo_library_outlined),
            title: const Text('앨범에서 선택'),
            onTap: () => Navigator.pop(ctx, ImageSource.gallery),
          ),
          if (allowCamera)
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.photo_camera_outlined),
              title: const Text('카메라로 촬영'),
              onTap: () => Navigator.pop(ctx, ImageSource.camera),
            ),
        ]),
      ),
    ),
  );

  if (choice is String) {
    final ByteData data = await rootBundle.load(choice);
    return XFile.fromData(
      data.buffer.asUint8List(),
      name: choice.split('/').last,
      mimeType: 'image/png',
    );
  }
  if (choice is ImageSource) {
    return ImagePicker().pickImage(source: choice, maxWidth: maxWidth, imageQuality: 85);
  }
  return null;
}
