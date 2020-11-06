package com.baidu.disk;

import com.baidu.disk.requester.LinkHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;


@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private LinkHelper linkHelper;

	@Test
	void contextLoads() throws IOException {
		System.out.println(linkHelper.getLink("0abdc1ba8e64bffdac5d3ccf935a66d6", "86214459-250528-140098086493323", "1604634976", "FDtAERVJoK-DCb740ccc5511e5e8fedcff06b081203-BnxgagkhhZDP32ZDuaHTNfb9I5A=", "3350578322"));
	}

}
