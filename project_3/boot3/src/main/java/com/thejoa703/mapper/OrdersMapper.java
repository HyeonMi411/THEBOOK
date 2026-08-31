package com.thejoa703.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.thejoa703.entity.Orders;

@Mapper
public interface OrdersMapper {

	Orders findById(Long id);

	Orders findByTid(String tid);

	// 내 주문내역 - 페이징 (map : { userId, start, end }), hiddenByUser=false 인 것만
	List<Orders> findByUserId(Map<String, Object> map);

	int countByUserId(Long userId);

	void insert(Orders order);

	void update(Orders order);

	void deleteById(Long id);
}
