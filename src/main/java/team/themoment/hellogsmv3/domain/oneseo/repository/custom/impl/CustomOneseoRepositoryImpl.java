package team.themoment.hellogsmv3.domain.oneseo.repository.custom.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.TestResultTag;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.AdmissionTicketsResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.SearchOneseoResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.ScreeningCategory;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.custom.CustomOneseoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.querydsl.core.types.ExpressionUtils.anyOf;
import static team.themoment.hellogsmv3.domain.member.entity.QMember.member;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QEntranceTestFactorsDetail.entranceTestFactorsDetail;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QEntranceTestResult.entranceTestResult;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QMiddleSchoolAchievement.middleSchoolAchievement;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QOneseo.oneseo;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QOneseoPrivacyDetail.oneseoPrivacyDetail;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.YES;

@Repository
@RequiredArgsConstructor
public class CustomOneseoRepositoryImpl implements CustomOneseoRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdmissionTicketsResDto> findAdmissionTickets() {
        return queryFactory.select(
                        Projections.constructor(
                                AdmissionTicketsResDto.class,
                                oneseo.member.name,
                                oneseo.member.birth,
                                oneseoPrivacyDetail.profileImg,
                                oneseoPrivacyDetail.schoolName,
                                oneseo.examinationNumber,
                                oneseo.oneseoSubmitCode
                        )
                )
                .from(oneseo)
                .join(oneseo.member, member)
                .join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail)
                .join(oneseo.entranceTestResult, entranceTestResult)
                .where(entranceTestResult.firstTestPassYn.eq(YES))
                .fetch();
    }

    @Override
    public Optional<Oneseo> findByGuardianOrTeacherPhoneNumberAndSubmitCode(String phoneNumber, String submitCode) {
        return Optional.ofNullable(
                queryFactory.selectFrom(oneseo)
                        .join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail)
                        .where(
                                oneseoPrivacyDetail.guardianPhoneNumber.eq(phoneNumber)
                                        .or(oneseoPrivacyDetail.schoolTeacherPhoneNumber.eq(phoneNumber))
                                        .and(oneseo.oneseoSubmitCode.eq(submitCode))
                        ).fetchOne()
        );
    }

    @Override
    public Optional<Oneseo> findByGuardianOrTeacherPhoneNumberAndExaminationNumber(String phoneNumber, String examinationNumber) {
         return Optional.ofNullable(
                 queryFactory.selectFrom(oneseo)
                    .join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail)
                    .where(
                            oneseoPrivacyDetail.guardianPhoneNumber.eq(phoneNumber)
                                    .or(oneseoPrivacyDetail.schoolTeacherPhoneNumber.eq(phoneNumber))
                                    .and(oneseo.examinationNumber.eq(examinationNumber))
                    ).fetchOne()
         );
    }

    @Override
    public Integer findMaxSubmitCodeByScreening(Screening screening) {
        return queryFactory
                .select(oneseo.oneseoSubmitCode.substring(2).castToNum(Integer.class))
                .from(oneseo)
                .where(oneseo.wantedScreening.eq(screening))
                .orderBy(oneseo.oneseoSubmitCode.substring(2).castToNum(Integer.class).desc())
                .fetchFirst();
    }

    @Override
    public Page<SearchOneseoResDto> findAllByKeywordAndScreeningAndSubmissionStatusAndTestResult(
            String keyword,
            ScreeningCategory screeningTag,
            YesNo isSubmitted,
            TestResultTag testResultTag,
            Pageable pageable
    ) {

        BooleanBuilder builder = createBooleanBuilder(
                keyword,
                screeningTag,
                isSubmitted,
                testResultTag
        );

        List<SearchOneseoResDto> oneseos = queryFactory
                .select(Projections.constructor(
                        SearchOneseoResDto.class,
                        oneseo.member.id,
                        oneseo.oneseoSubmitCode,
                        oneseo.realOneseoArrivedYn,
                        oneseo.member.name,
                        oneseo.wantedScreening,
                        oneseo.oneseoPrivacyDetail.schoolName,
                        oneseo.member.phoneNumber,
                        oneseo.oneseoPrivacyDetail.guardianPhoneNumber,
                        oneseo.oneseoPrivacyDetail.schoolTeacherPhoneNumber,
                        oneseo.entranceTestResult.firstTestPassYn,
                        oneseo.entranceTestResult.competencyEvaluationScore,
                        oneseo.entranceTestResult.interviewScore,
                        oneseo.entranceTestResult.secondTestPassYn,
                        oneseo.entranceIntentionYn
                ))
                .from(oneseo)
                .join(oneseo.member, member)
                .join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail)
                .join(oneseo.entranceTestResult, entranceTestResult)
                .where(builder)
                .orderBy(oneseo.oneseoSubmitCode.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return PageableExecutionUtils.getPage(oneseos, pageable, () -> getTotalCount(builder));
    }

    @Override
    public Optional<Oneseo> findByMemberNameAndMemberBirthAndPhoneNumber(String memberName, String phoneNumber, LocalDate memberBirth) {
        return Optional.ofNullable(
                queryFactory.selectFrom(oneseo)
                        .leftJoin(oneseo.member, member)
                        .fetchJoin()
                        .leftJoin(oneseo.entranceTestResult, entranceTestResult)
                        .fetchJoin()
                        .leftJoin(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail)
                        .fetchJoin()
                        .leftJoin(oneseo.middleSchoolAchievement, middleSchoolAchievement)
                        .fetchJoin()
                        .where(
                                member.name.eq(memberName)
                                        .and(member.phoneNumber.eq(phoneNumber))
                                        .and(member.birth.eq(memberBirth))
                        ).fetchOne()
        );
    }

    @Override
    public List<Oneseo> findAllByScreeningWithAllDetails(Screening screening) {
        boolean isExistAppliedScreening = queryFactory
                .selectOne()
                .from(oneseo)
                .where(oneseo.appliedScreening.isNotNull())
                .fetchFirst() != null;

        return queryFactory
                .selectFrom(oneseo)
                .join(oneseo.member, member).fetchJoin()
                .join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail).fetchJoin()
                .join(oneseo.entranceTestResult, entranceTestResult).fetchJoin()
                .join(entranceTestResult.entranceTestFactorsDetail, entranceTestFactorsDetail).fetchJoin()
                .leftJoin(oneseo.middleSchoolAchievement, middleSchoolAchievement).fetchJoin()
                .where(isExistAppliedScreening
                        ? oneseo.appliedScreening.eq(screening)
                        : oneseo.wantedScreening.eq(screening))
                .orderBy(entranceTestResult.documentEvaluationScore.desc())
                .fetch();
    }

    @Override
    public List<Oneseo> findAllFailedWithAllDetails() {
        return queryFactory
                .selectFrom(oneseo)
                .join(oneseo.member, member).fetchJoin()
                .join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail).fetchJoin()
                .join(oneseo.entranceTestResult, entranceTestResult).fetchJoin()
                .join(entranceTestResult.entranceTestFactorsDetail, entranceTestFactorsDetail).fetchJoin()
                .leftJoin(oneseo.middleSchoolAchievement, middleSchoolAchievement).fetchJoin()
                .where(entranceTestResult.firstTestPassYn.eq(NO)
                        .or(entranceTestResult.secondTestPassYn.eq(NO)))
                .orderBy(entranceTestResult.documentEvaluationScore.desc())
                .fetch();
    }

    private long getTotalCount(BooleanBuilder builder) {
        return queryFactory
                .select(oneseo.count())
                .from(oneseo)
                .where(builder)
                .fetchFirst();
    }

    private BooleanBuilder createBooleanBuilder(
            String keyword,
            ScreeningCategory screeningTag,
            YesNo isSubmitted,
            TestResultTag testResultTag
    ) {

        BooleanBuilder builder = new BooleanBuilder();
        applyKeyword(builder, keyword);
        applyScreeningTag(builder, screeningTag);
        applyIsSubmittedTag(builder, isSubmitted);
        applyTestResultTag(builder, testResultTag);

        return builder;
    }

    private void applyKeyword(
            BooleanBuilder builder,
            String keyword
    ) {
        if (keyword == null) return;

        builder.and(
                anyOf(
                        oneseo.member.name.like("%" + keyword + "%"),
                        oneseoPrivacyDetail.schoolName.like("%" + keyword + "%"),
                        oneseo.member.phoneNumber.like("%" + keyword + "%")
                )
        );
    }

    private void applyScreeningTag(
            BooleanBuilder builder,
            ScreeningCategory screeningTag
    ) {
        if (screeningTag == null) return;

        switch (screeningTag) {
            case GENERAL ->
                    builder.and(
                            oneseo.wantedScreening.eq(Screening.GENERAL)
                    );
            case SPECIAL ->
                    builder.and(
                            oneseo.wantedScreening.eq(Screening.SPECIAL)
                    );
            case EXTRA ->
                    builder.andAnyOf(
                            oneseo.wantedScreening.eq(Screening.EXTRA_ADMISSION),
                            oneseo.wantedScreening.eq(Screening.EXTRA_VETERANS)
                    );
        }
    }

    private void applyIsSubmittedTag(
            BooleanBuilder builder,
            YesNo isSubmitted
    ) {
        if (isSubmitted == null) return;


        switch (isSubmitted) {
            case YES ->
                    builder.and(
                            oneseo.realOneseoArrivedYn.eq(YES)
                    );
            case NO ->
                    builder.and(
                            oneseo.realOneseoArrivedYn.eq(NO)
                    );
        }
    }

    private void applyTestResultTag(
            BooleanBuilder builder,
            TestResultTag testResultTag
    ) {

        switch (testResultTag) {
            case FIRST_PASS ->
                    builder.and(
                            entranceTestResult.firstTestPassYn.eq(YES)
                    );
            case FINAL_PASS ->
                    builder.and(
                            entranceTestResult.secondTestPassYn.eq(YES)
                    );
            case FALL ->
                    builder.andAnyOf(
                            entranceTestResult.firstTestPassYn.eq(NO),
                            entranceTestResult.secondTestPassYn.eq(NO)
                    );
        }
    }
    @Override
    public Map<String,EntranceTestResult> findEntranceTestResultByExaminationNumbersIn(List<String> examinationNumbers) {
        return queryFactory.selectFrom(entranceTestResult)
                .select(oneseo.examinationNumber, entranceTestResult)
                .join(entranceTestResult.oneseo, oneseo)
                .where(oneseo.examinationNumber.in(examinationNumbers))
                .fetch()
                .stream()
                .collect(
                        Collectors.toMap(
                                tuple -> tuple.get(oneseo.examinationNumber),
                                tuple -> tuple.get(entranceTestResult)
                        )
                );
    }
}
