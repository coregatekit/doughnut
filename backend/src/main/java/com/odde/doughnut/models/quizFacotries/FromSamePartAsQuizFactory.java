package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.*;
import java.util.stream.Collectors;

public class FromSamePartAsQuizFactory implements QuizQuestionFactory {
    private Link cachedAnswerLink = null;
    private List<Note> cachedFillingOptions = null;
    protected final QuizQuestionServant servant;
    protected final ReviewPoint reviewPoint;
    protected final Link link;

    public FromSamePartAsQuizFactory(QuizQuestionServant servant, ReviewPoint reviewPoint) {
        this.servant = servant;
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    @Override
    public List<Note> generateFillingOptions() {
        if (cachedFillingOptions == null) {
            cachedFillingOptions = link.getRemoteCousinOfDifferentCategory(reviewPoint.getUser())
                    .map(links ->
                            servant.randomizer.randomlyChoose(5, links).stream()
                                    .map(Link::getSourceNote).collect(Collectors.toList())
                    ).orElse(Collections.emptyList());
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one's <mark>" + link.categoryLink().map(l -> l.getTargetNote().getTitle()).orElse("") + "</mark> is the same as:";
    }

    @Override
    public String generateMainTopic() {
        return link.getSourceNote().getTitle();
    }

    @Override
    public Note generateAnswerNote() {
        if (getAnswerLink() == null) return null;
        return getAnswerLink().getSourceNote();
    }

    @Override
    public List<QuizQuestion.Option> toQuestionOptions(List<Note> notes) {
        return servant.toTitleOptions(notes);
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }

    @Override
    public boolean isValidQuestion() {
        return generateAnswerNote() != null && !getViceReviewPoints().isEmpty() && generateFillingOptions().size() > 0;
    }

    @Override
    public List<ReviewPoint> getViceReviewPoints() {
        Link answerLink = getAnswerLink();
        if (answerLink != null) {
            UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
            ReviewPoint answerLinkReviewPoint = userModel.getReviewPointFor(cachedAnswerLink);
            List<ReviewPoint> result = new ArrayList<>();
            result.add(answerLinkReviewPoint);
            link.categoryLink().ifPresent(l -> {
                ReviewPoint reviewPointFor = userModel.getReviewPointFor(l);
                if (reviewPointFor != null) {
                    result.add(reviewPointFor);
                }
            });
            return result;
        }
        return Collections.emptyList();
    }

    protected Link getAnswerLink() {
        if (cachedAnswerLink == null) {
            UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
            List<Link> backwardPeers = link.getCousinLinks(reviewPoint.getUser()).stream()
                    .filter(l->userModel.getReviewPointFor(l) != null).collect(Collectors.toUnmodifiableList());
            cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers);
        }
        return cachedAnswerLink;
    }

}