package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class UserModelAuthorityTest {
    @Autowired
    MakeMe makeMe;
    UserModel userModel;
    UserModel anotherUser;

    @BeforeEach
    void setup() {
        userModel = makeMe.aUser().toModelPlease();
        anotherUser = makeMe.aUser().toModelPlease();
    }

    @Nested
    class noteBelongsToACircle {
        CircleModel circleModel;
        Note note;

        @BeforeEach
        void setup() {
            circleModel = makeMe.aCircle().toModelPlease();
            note = makeMe.aNote().byUser(makeMe.aUser().please()).inCircle(circleModel).please();
        }

        @Test
        void userCanNotAccessNotesBelongToCircle() {
            assertFalse(userModel.getAuthorization().hasFullAuthority(note));
        }

        @Test
        void userCanAccessNotesBelongToCircleIfIsAMember() {
            makeMe.theCircle(circleModel).hasMember(userModel).please();
            assertTrue(userModel.getAuthorization().hasFullAuthority(note));
        }
    }

    @Nested
    class failureReportNotAccess{

        UserModel userModel;
        @BeforeEach
        void setup() {
            this.userModel = makeMe.aUser().toModelPlease();
        }

        @Test
        void userCanAccessFailureReports() {
            this.userModel.setDeveloper(true);
            assertTrue(this.userModel.isDeveloper());
        }

        @Test
        void userCanNotAccessFailureReports() {
            this.userModel.setDeveloper(false);
            assertFalse(this.userModel.isDeveloper());
        }

        @ParameterizedTest
        @CsvSource({
            "satou, true",
            "matsumura, true",
            "hoge, false",
            "fuga, false"
        })
        void developerCanAccessFailureReports(String name, boolean isDev) {
            User user = new User();
            user.setName(name);
            this.userModel = makeMe.modelFactoryService.toUserModel(user);
            assertEquals(isDev, this.userModel.isDeveloper());
        }
    }
}

