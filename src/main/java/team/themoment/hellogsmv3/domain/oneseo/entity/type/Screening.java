package team.themoment.hellogsmv3.domain.oneseo.entity.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum Screening {
    GENERAL(ScreeningCategory.GENERAL),
    SPECIAL(ScreeningCategory.SPECIAL),
    EXTRA_VETERANS(ScreeningCategory.EXTRA),
    EXTRA_ADMISSION(ScreeningCategory.EXTRA);

    private final ScreeningCategory screeningCategory;
    public static List<Screening> findAllByScreeningCategory(ScreeningCategory screeningCategory) {
        return Stream.of(Screening.values())
                .filter(screening -> screening.getScreeningCategory() == screeningCategory)
                .toList();
    }
}
