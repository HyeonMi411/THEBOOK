package com.thejoa703.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
SQL> desc sboard2;
 이름                                      널?      유형
 ----------------------------------------- -------- ----------------------------
 ID                                        NOT NULL NUMBER
 APP_USER_ID                               NOT NULL NUMBER
 BTITLE                                    NOT NULL VARCHAR2(1000)
 BCONTENT                                  NOT NULL CLOB
 BPASS                                     NOT NULL VARCHAR2(255)
 BFILE                                              VARCHAR2(255)
 BHIT                                               NUMBER
 BIP                                       NOT NULL VARCHAR2(255)
 CREATED_AT                                         DATE
*/

@Entity
@Table(name = "SBOARD2")
@Getter @Setter @NoArgsConstructor
@AllArgsConstructor @Builder
public class Sboard2 {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sboard2_seq")
    @SequenceGenerator(name = "sboard2_seq", sequenceName = "SBOARD2_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    // ★ 한 유저가 여러 글을 쓸 수 있다  (다대일)
    // - AppUser.boards (mappedBy="user") 쪽과 연결
    @ManyToOne
    @JoinColumn(name = "APP_USER_ID", nullable = false)
    private AppUser user;

    @Column(name = "BTITLE", length = 1000, nullable = false)
    private String btitle;

    @Lob
    @Column(name = "BCONTENT", nullable = false)
    private String bcontent;

    @Column(name = "BPASS", length = 255, nullable = false)
    private String bpass;

    @Column(name = "BFILE", length = 255)
    private String bfile;

    @Column(name = "BHIT")
    private Integer bhit;

    @Column(name = "BIP", length = 255, nullable = false)
    private String bip;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.bhit == null) {
            this.bhit = 0;
        }
    }
}
