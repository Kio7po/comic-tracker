package com.github.kio7po.comic_tracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ComicTrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
