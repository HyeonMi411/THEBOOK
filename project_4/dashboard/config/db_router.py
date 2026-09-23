"""
analytics 앱의 모델(ORDERS/ORDER_ITEMS/BOOK)은 전부 Spring Boot 소유의
Oracle DB를 "읽기 전용"으로 매핑한 것이므로, 이 라우터로 강제 분리한다.
- analytics 모델 → 반드시 'oracle' DB로만 읽기 (default DB로 잘못 조회되는 것 방지)
- analytics 모델에 대한 쓰기(마이그레이션 포함)는 전부 차단 → Spring Boot 스키마를
  Django가 실수로 건드리는 사고를 원천 차단
"""


class OracleReadOnlyRouter:
    def db_for_read(self, model, **hints):
        if model._meta.app_label == "analytics":
            return "oracle"
        return None

    def db_for_write(self, model, **hints):
        if model._meta.app_label == "analytics":
            # 읽기 전용 - 절대 오라클에 쓰지 않음. auth 등 자체 모델만 default(sqlite)에 씀.
            raise RuntimeError("analytics 앱은 읽기 전용입니다 (Spring Boot 소유 DB).")
        return None

    def allow_migrate(self, db, app_label, model_name=None, **hints):
        if app_label == "analytics":
            # Spring Boot(JPA)가 이미 만든 테이블이므로 Django가 마이그레이션하면 안 됨
            return False
        # 나머지(auth, admin, sessions 등)는 default(sqlite)에만 마이그레이션
        return db == "default"
