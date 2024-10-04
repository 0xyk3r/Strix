package cn.projectan.strix.model.other.country;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ProjectAn
 * @since 2022/4/2 18:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountryInfo {

    private String name;

    private String twoCode;

    private String threeCode;

}
