import 'package:dio/dio.dart';
import 'package:image_picker/image_picker.dart';

const Map<String, String> _imageTypes = {
  'jpg': 'image/jpeg',
  'jpeg': 'image/jpeg',
  'png': 'image/png',
  'gif': 'image/gif',
  'webp': 'image/webp',
};

/// 이미지 업로드용 multipart 파트
///
/// 서버(FileStorageService)는 Content-Type 이 image/jpeg·png·gif·webp 인지 검사한다.
/// Dio 의 MultipartFile.fromBytes 는 형식을 지정하지 않으면 application/octet-stream 으로 보내서
/// "허용되지 않는 파일 형식" 으로 거절되므로, 파일 이름(확장자)으로 형식을 정해서 보낸다.
Future<MultipartFile> imagePart(XFile file) async {
  String name = file.name.isNotEmpty ? file.name : 'image.jpg';
  String ext = name.contains('.') ? name.split('.').last.toLowerCase() : '';
  if (!_imageTypes.containsKey(ext)) {
    // 확장자가 없거나 heic 등 → 서버가 받는 jpg 로 이름을 맞춤 (image_picker 가 압축하면 jpeg 로 저장됨)
    ext = 'jpg';
    name = '${name.split('.').first}.jpg';
  }
  final String mime = (file.mimeType != null && _imageTypes.containsValue(file.mimeType)) ? file.mimeType! : _imageTypes[ext]!;
  return MultipartFile.fromBytes(await file.readAsBytes(), filename: name, contentType: DioMediaType.parse(mime));
}
