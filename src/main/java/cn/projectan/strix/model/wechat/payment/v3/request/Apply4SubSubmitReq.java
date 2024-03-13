package cn.projectan.strix.model.wechat.payment.v3.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 特约商户进件-提交申请单 请求
 *
 * @author ProjectAn
 * @date 2022/7/22 17:46
 */
@Data
@Accessors(chain = true)
public class Apply4SubSubmitReq {

    /**
     * 业务申请编号
     */
    private String business_code;

    /**
     * 超级管理员信息
     */
    private ContactInfo contact_info;

    /**
     * 主体资料
     */
    private SubjectInfo subject_info;

    /**
     * 经营资料
     */
    private BusinessInfo business_info;

    /**
     * 结算规则
     */
    private SettlementInfo settlement_info;

    /**
     * 结算银行账户
     */
    private BankAccountInfo bank_account_info;

    /**
     * 补充材料
     */
    private AdditionInfo addition_info;


    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContactInfo {

        /**
         * 超级管理员类型
         * LEGAL 经营者/法人
         * SUPER 经办人
         */
        private String contact_type;

        /**
         * 超级管理员姓名
         */
        private String contact_name;

        /**
         * 超级管理员证件类型
         * 当超级管理员类型是经办人时，必填。
         * IDENTIFICATION_TYPE_IDCARD：中国大陆居民-身份证
         * IDENTIFICATION_TYPE_OVERSEA_PASSPORT：其他国家或地区居民-护照
         * IDENTIFICATION_TYPE_HONGKONG_PASSPORT：中国香港居民-来往内地通行证
         * IDENTIFICATION_TYPE_MACAO_PASSPORT：中国澳门居民-来往内地通行证
         * IDENTIFICATION_TYPE_TAIWAN_PASSPORT：中国台湾居民-来往大陆通行证
         * IDENTIFICATION_TYPE_FOREIGN_RESIDENT：外国人居留证
         * IDENTIFICATION_TYPE_HONGKONG_MACAO_RESIDENT：港澳居民证
         * IDENTIFICATION_TYPE_TAIWAN_RESIDENT：台湾居民证
         */
        private String contact_id_doc_type;

        /**
         * 超级管理员身份证件号码
         * 当超级管理员类型是经办人时，必填。
         */
        private String contact_id_number;

        /**
         * 超级管理员证件正面照片
         * 当超级管理员类型是经办人时，必填。
         */
        private String contact_id_doc_copy;

        /**
         * 超级管理员证件反面照片
         * 当超级管理员类型是经办人时，必填。
         */
        private String contact_id_doc_copy_back;

        /**
         * 超级管理员证件有效期开始时间
         * 当超级管理员类型是经办人时，必填。
         */
        private String contact_period_begin;

        /**
         * 超级管理员证件有效期结束时间
         * 当超级管理员类型是经办人时，必填。
         */
        private String contact_period_end;

        /**
         * 业务办理授权函
         * 当超级管理员类型是经办人时，必填。
         * 请参照<a href="https://file.service.qq.com/user-files/uploads/202206/bd03de40bbe1015a3016812cde2e1f3a.png">[示例图]</a>打印业务办理授权函，全部信息需打印，不支持手写商户信息，并加盖公章。
         */
        private String business_authorization_letter;

        /**
         * 超级管理员微信OpenID
         */
        private String openid;

        /**
         * 联系手机
         * 用于接收微信支付的重要管理信息及日常操作验证码
         */
        private String mobile_phone;

        /**
         * 联系邮箱
         * 用于接收微信支付的开户邮件及日常业务通知
         */
        private String contact_email;

    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubjectInfo {

        /**
         * 主体类型
         * SUBJECT_TYPE_INDIVIDUAL（个体户）：营业执照上的主体类型一般为个体户、个体工商户、个体经营；
         * SUBJECT_TYPE_ENTERPRISE（企业）：营业执照上的主体类型一般为有限公司、有限责任公司；
         * SUBJECT_TYPE_GOVERNMENT （政府机关）：包括各级、各类政府机关，如机关党委、税务、民政、人社、工商、商务、市监等；
         * SUBJECT_TYPE_INSTITUTIONS（事业单位）：包括国内各类事业单位，如：医疗、教育、学校等单位；
         * SUBJECT_TYPE_OTHERS（社会组织）： 包括社会团体、民办非企业、基金会、基层群众性自治组织、农村集体经济组织等组织。
         */
        private String subject_type;

        /**
         * 是否是金融机构
         */
        private Boolean finance_institution;

        /**
         * 营业执照
         * 主体为个体户/企业，必填
         */
        private BusinessLicenseInfo business_license_info;

        /**
         * 登记证书
         * 主体为政府机关/事业单位/其他组织时，必填。
         */
        private CertificateInfo certificate_info;

        /**
         * 单位证明函照片
         * 1、主体类型为政府机关、事业单位选传：
         * （1）若上传，则审核通过后即可签约，无需汇款验证。
         * （2）若未上传，则审核通过后，需汇款验证。
         * 2、主体为个体户、企业、其他组织等，不需要上传本字段。
         * 3、请参照<a href="https://file.service.qq.com/user-files/uploads/202202/1ff900980435576570e3f327cf48b128.png">示例图</a>打印单位证明函，全部信息需打印，不支持手写商户信息，并加盖公章。
         */
        private String certificate_letter_copy;

        /**
         * 金融机构许可证信息
         * 当主体是金融机构时，必填。
         */
        private FinanceInstitutionInfo finance_institution_info;

        /**
         * 经营者/法人身份证件
         * 1、个体户：请上传经营者的身份证件。
         * 2、企业/社会组织：请上传法人的身份证件。
         * 3、政府机关/事业单位：请上传法人/经办人的身份证件。
         */
        private IdentityInfo identity_info;

        /**
         * 仅企业需要填写
         * 若经营者/法人不是最终受益所有人，则需补充受益所有人信息，最多上传4个。
         * 若经营者/法人是最终受益所有人之一，可在此添加其他受益所有人信息，最多上传3个。
         * 根据国家相关法律法规，需要提供公司受益所有人信息，受益所有人需符合至少以下条件之一：
         * 1、直接或者间接拥有超过25%公司股权或者表决权的自然人。
         * 2、通过人事、财务等其他方式对公司进行控制的自然人。
         * 3、公司的高级管理人员，包括公司的经理、副经理、财务负责人、上市公司董事会秘书和公司章程规定的其他人员。
         */
        private List<UboInfoList> ubo_info_list;

        @Data
        public static class BusinessLicenseInfo {

            /**
             * 营业执照照片
             */
            private String license_copy;

            /**
             * 注册号/统一社会信用代码
             * 注册号格式须为18位数字|大写字母
             */
            private String license_number;

            /**
             * 商户名称
             * 个体户，不能以“公司”结尾
             * 个体户，若营业执照上商户名称为空或为“无”，请填写"个体户+经营者姓名"，如“个体户张三”
             */
            private String merchant_name;

            /**
             * 个体户经营者/法人姓名
             */
            private String legal_person;

            /**
             * 注册地址
             * 选填
             */
            private String license_address;

            /**
             * 有效期限开始日期
             * 选填
             */
            private String period_begin;

            /**
             * 有效期限结束日期
             * 选填
             */
            private String period_end;

        }

        @Data
        public static class CertificateInfo {

            /**
             * 登记证书照片
             */
            private String cert_copy;

            /**
             * 登记证书类型
             * <p>
             * 1、主体为“政府机关/事业单位/社会组织”时，请上传登记证书类型。
             * 2、主体为“个体工商户/企业”时，不填。
             * 当主体为事业单位时，选择此枚举值：
             * CERTIFICATE_TYPE_2388：事业单位法人证书
             * <p>
             * 当主体为政府机关，选择此枚举值：
             * CERTIFICATE_TYPE_2389：统一社会信用代码证书
             * <p>
             * 当主体为社会组织，选择以下枚举值之一：
             * CERTIFICATE_TYPE_2389：统一社会信用代码证书
             * CERTIFICATE_TYPE_2394：社会团体法人登记证书
             * CERTIFICATE_TYPE_2395：民办非企业单位登记证书
             * CERTIFICATE_TYPE_2396：基金会法人登记证书
             * CERTIFICATE_TYPE_2397：慈善组织公开募捐资格证书(已废弃)
             * CERTIFICATE_TYPE_2398：农民专业合作社法人营业执照(已废弃)
             * CERTIFICATE_TYPE_2520：执业许可证/执业证
             * CERTIFICATE_TYPE_2521：基层群众性自治组织特别法人统一社会信用代码证
             * CERTIFICATE_TYPE_2522：农村集体经济组织登记证
             * CERTIFICATE_TYPE_2399：宗教活动场所登记证
             * CERTIFICATE_TYPE_2400：政府部门下发的其他有效证明文件
             */
            private String cert_type;

            /**
             * 证书号
             */
            private String cert_number;

            /**
             * 商户名称
             */
            private String merchant_name;

            /**
             * 注册地址
             */
            private String company_address;

            /**
             * 法定代表人
             */
            private String legal_person;

            /**
             * 有效期限开始日期
             */
            private String period_begin;

            /**
             * 有效期限结束日期
             */
            private String period_end;

        }

        @Data
        public static class FinanceInstitutionInfo {

            /**
             * 金融机构类型
             * BANK_AGENT：银行业, 适用于商业银行、政策性银行、农村合作银行、村镇银行、开发性金融机构等
             * PAYMENT_AGENT：支付机构, 适用于非银行类支付机构
             * INSURANCE：保险业, 适用于保险、保险中介、保险代理、保险经纪等保险类业务
             * TRADE_AND_SETTLE：交易及结算类金融机构, 适用于交易所、登记结算类机构、银行卡清算机构、资金清算中心等
             * OTHER：其他金融机构, 适用于财务公司、信托公司、金融资产管理公司、金融租赁公司、汽车金融公司、贷款公司、货币经纪公司、消费金融公司、证券业、金融控股公司、股票、期货、货币兑换、小额贷款公司、金融资产管理、担保公司、商业保理公司、典当行、融资租赁公司、财经咨询等其他金融业务
             */
            private String finance_type;

            /**
             * 金融机构许可证图片
             * 最多可上传5张照片
             */
            private List<String> finance_license_pics;

        }

        @Data
        public static class IdentityInfo {

            /**
             * 证件持有人类型
             * 主体类型为政府机关、事业单位时选传：
             * （1）若上传的是法人证件，则不需要上传该字段
             * （2）若因特殊情况，无法提供法人证件时，可上传经办人。 （经办人：经商户授权办理微信支付业务的人员，授权范围包括但不限于签约，入驻过程需完成账户验证）。
             * 2. 主体类型为企业、个体户、社会组织时，默认为经营者/法人，不需要上传该字段。
             * LEGAL：法人
             * SUPER：经办人
             */
            private String id_holder_type;

            /**
             * 证件类型
             * 当证件持有人类型为法人时，填写。其他情况，无需上传。
             * 2、个体户/企业/事业单位/社会组织：可选择任一证件类型，政府机关仅支持中国大陆居民-身份证类型。
             * IDENTIFICATION_TYPE_IDCARD：中国大陆居民-身份证
             * IDENTIFICATION_TYPE_OVERSEA_PASSPORT：其他国家或地区居民-护照
             * IDENTIFICATION_TYPE_HONGKONG_PASSPORT：中国香港居民-来往内地通行证
             * IDENTIFICATION_TYPE_MACAO_PASSPORT：中国澳门居民-来往内地通行证
             * IDENTIFICATION_TYPE_TAIWAN_PASSPORT：中国台湾居民-来往大陆通行证
             * IDENTIFICATION_TYPE_FOREIGN_RESIDENT：外国人居留证
             * IDENTIFICATION_TYPE_HONGKONG_MACAO_RESIDENT：港澳居民证
             * IDENTIFICATION_TYPE_TAIWAN_RESIDENT：台湾居民证
             */
            private String id_doc_type;

            /**
             * 法定代表人说明函
             * 当证件持有人类型为经办人时，必须上传。其他情况，无需上传。
             * 若因特殊情况，无法提供法定代表人证件时，请参照<a href="https://file.service.qq.com/user-files/uploads/202205/0706396feac98f2c31b0a21c04369471.png">示例图</a>打印法定代表人说明函，全部信息需打印，不支持手写商户信息，并加盖公章。
             */
            private String authorize_letter_copy;

            /**
             * 当证件持有人类型为经营者/法人且证件类型为“身份证”时填写。
             */
            private IdCardInfo id_card_info;

            /**
             * 当证件持有人类型为经营者/法人且证件类型不为“身份证”时填写。
             */
            private IdDocInfo id_doc_info;

            /**
             * 经营者/法人是否为受益人
             */
            private Boolean owner;


            @Data
            public static class IdCardInfo {

                /**
                 * 身份证人像面照片
                 */
                private String id_card_copy;

                /**
                 * 身份证国徽面照片
                 */
                private String id_card_national;

                /**
                 * 身份证姓名
                 */
                private String id_card_name;

                /**
                 * 身份证号码
                 */
                private String id_card_number;

                /**
                 * 身份证居住地址
                 */
                private String id_card_address;

                /**
                 * 身份证有效期开始时间
                 */
                private String card_period_begin;

                /**
                 * 身份证有效期结束时间
                 */
                private String card_period_end;

            }

            @Data
            public static class IdDocInfo {

                /**
                 * 证件正面照片
                 */
                private String id_doc_copy;

                /**
                 * 证件反面照片
                 */
                private String id_doc_copy_back;

                /**
                 * 证件姓名
                 */
                private String id_doc_name;

                /**
                 * 证件号码
                 */
                private String id_doc_number;

                /**
                 * 证件居住地址
                 */
                private String id_doc_address;

                /**
                 * 证件有效期开始时间
                 */
                private String doc_period_begin;

                /**
                 * 证件有效期结束时间
                 */
                private String doc_period_end;

            }

        }

        @Data
        public static class UboInfoList {

            /**
             * 证件类型
             * IDENTIFICATION_TYPE_IDCARD：中国大陆居民-身份证
             * IDENTIFICATION_TYPE_OVERSEA_PASSPORT：其他国家或地区居民-护照
             * IDENTIFICATION_TYPE_HONGKONG_PASSPORT：中国香港居民-来往内地通行证
             * IDENTIFICATION_TYPE_MACAO_PASSPORT：中国澳门居民-来往内地通行证
             * IDENTIFICATION_TYPE_TAIWAN_PASSPORT：中国台湾居民-来往大陆通行证
             * IDENTIFICATION_TYPE_FOREIGN_RESIDENT：外国人居留证
             * IDENTIFICATION_TYPE_HONGKONG_MACAO_RESIDENT：港澳居民证
             * IDENTIFICATION_TYPE_TAIWAN_RESIDENT：台湾居民证
             */
            private String ubo_id_doc_type;

            /**
             * 证件正面照片
             */
            private String ubo_id_doc_copy;

            /**
             * 证件反面照片
             */
            private String ubo_id_doc_copy_back;

            /**
             * 证件姓名
             */
            private String ubo_id_doc_name;

            /**
             * 证件号码
             */
            private String ubo_id_doc_number;

            /**
             * 证件居住地址
             */
            private String ubo_id_doc_address;

            /**
             * 证件有效期开始时间
             */
            private String ubo_period_begin;

            /**
             * 证件有效期结束时间
             */
            private String ubo_period_end;

        }

    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BusinessInfo {

        /**
         * 商户简称
         * 在支付完成页向买家展示，需与微信经营类目相关
         * （1）不支持单纯以人名来命名，若为个体户经营，可用“个体户+经营者名称”或“经营者名称+业务”命名，如“个体户张三”或“张三餐饮店”；
         */
        private String merchant_shortname;

        /**
         * 客服电话
         * 将在交易记录中向买家展示，请确保电话畅通以便平台回拨确认
         */
        private String service_phone;

        /**
         * 经营场景
         */
        private SalesInfo sales_info;

        @Data
        public static class SalesInfo {

            /**
             * 经营场景类型
             * 1、请勾选实际售卖商品/提供服务场景（至少一项），以便为你开通需要的支付权限。
             * 2、建议只勾选目前必须的场景，以便尽快通过入驻审核，其他支付权限可在入驻后再根据实际需要发起申请。
             * SALES_SCENES_STORE：线下场所
             * SALES_SCENES_MP：公众号
             * SALES_SCENES_MINI_PROGRAM：小程序
             * SALES_SCENES_WEB：互联网网站
             * SALES_SCENES_APP：APP
             * SALES_SCENES_WEWORK：企业微信
             */
            private List<String> sales_scenes_type;

            /**
             * 线下场所场景
             * 1、审核通过后，服务商可帮商户发起付款码支付、JSAPI支付。
             * 2、当"经营场景类型"选择"SALES_SCENES_STORE"，该场景资料必填。
             */
            private BizStoreInfo biz_store_info;

            /**
             * 公众号场景
             * 1、审核通过后，服务商可帮商家发起JSAPI支付
             * 2、当"经营场景类型"选择"SALES_SCENES_MP"，该场景资料必填。
             */
            private MpInfo mp_info;

            /**
             * 小程序场景
             * 1、审核通过后，服务商可帮商家发起JSAPI支付
             * 2、当"经营场景类型"选择"SALES_SCENES_MINI_PROGRAM"，该场景资料必填。
             */
            private MiniProgramInfo mini_program_info;

            /**
             * App场景
             * 1、审核通过后，服务商可帮商家发起App支付
             * 2、当"经营场景类型"选择"SALES_SCENES_APP"，该场景资料必填。
             */
            private AppInfo app_info;

            /**
             * 互联网网站场景
             * 1、审核通过后，服务商可帮商家发起JSAPI支付、Native支付
             * 2、当"经营场景类型"选择"SALES_SCENES_WEB"，该场景资料必填。
             */
            private WebInfo web_info;

            /**
             * 企业微信场景
             * 1、审核通过后，服务商可帮商家发起企业微信支付
             * 2、当"经营场景类型"选择"SALES_SCENES_WEWORK"，该场景资料必填。
             */
            private WeworkInfo wework_info;

            @Data
            public static class BizStoreInfo {

                /**
                 * 线下场所名称
                 */
                private String biz_store_name;

                /**
                 * 线下场所省市编码
                 * excel
                 */
                private String biz_address_code;

                /**
                 * 线下场所地址
                 */
                private String biz_store_address;

                /**
                 * 线下场所门头照片
                 */
                private List<String> store_entrance_pic;

                /**
                 * 线下场所内部照片
                 */
                private List<String> indoor_pic;

                /**
                 * 线下场所对应的商家AppID
                 * 1、可填写与商家主体一致且已认证的公众号、小程序、APP的AppID，其中公众号AppID需是已认证的服务号、政府或媒体类型的订阅号；
                 * 2、审核通过后，系统将额外为商家开通付款码支付、JSAPI支付的自有交易权限，并完成商家商户号与该AppID的绑定；
                 */
                private String biz_sub_appid;

            }

            @Data
            public static class MpInfo {

                /**
                 * 服务商公众号AppID
                 * 1、服务商公众号APPID与商家公众号APPID，二选一必填。
                 * 2、可填写当前服务商商户号已绑定的公众号APPID。
                 */
                private String mp_appid;

                /**
                 * 商家公众号AppID
                 * 1、服务商公众号APPID与商家公众号APPID，二选一必填。
                 * 2、可填写与商家主体一致且已认证的公众号APPID，需是已认证的服务号、政府或媒体类型的订阅号。
                 * 3、审核通过后，系统将发起特约商家商户号与该AppID的绑定（即配置为sub_appid），服务商随后可在发起支付时选择传入该appid，以完成支付，并获取sub_openid用于数据统计，营销等业务场景 。
                 */
                private String mp_sub_appid;

                /**
                 * 公众号截图页面
                 * 1、请提供展示商品/服务的页面截图/设计稿（最多5张），若公众号未建设完善或未上线请务必提供。
                 * 2、请填写通过图片上传API预先上传图片生成好的MediaID。
                 */
                private List<String> mp_pics;

            }

            @Data
            public static class MiniProgramInfo {

                /**
                 * 服务商小程序AppID
                 * 1、服务商小程序APPID与商家小程序APPID，二选一必填。
                 * 2、可填写当前服务商商户号已绑定的小程序APPID。
                 */
                private String mini_program_appid;

                /**
                 * 商家小程序AppID
                 * 1、服务商小程序APPID与商家小程序APPID，二选一必填；
                 * 2、请填写已认证的小程序APPID；
                 * 3、完成进件后，系统发起特约商户号与该AppID的绑定（即配置为sub_appid可在发起支付时传入）
                 * （1）若APPID主体与商家主体/服务商主体一致，则直接完成绑定；
                 * （2）若APPID主体与商家主体/服务商主体不一致，则商户签约时显示《联合营运承诺函》，并且AppID的管理员需登录公众平台确认绑定意愿；
                 */
                private String mini_program_sub_appid;

                /**
                 * 小程序截图
                 * 1、请提供展示商品/服务的页面截图/设计稿（最多5张），若小程序未建设完善或未上线 请务必提供；
                 * 2、请填写通过图片上传API预先上传图片生成好的MediaID。
                 */
                private List<String> mini_program_pics;

            }

            @Data
            public static class AppInfo {

                /**
                 * 服务商应用AppID
                 * 1、服务商应用APPID与商家应用APPID，二选一必填。
                 * 2、可填写当前服务商商户号已绑定的应用APPID。
                 */
                private String app_appid;

                /**
                 * 商家应用AppID
                 * 1、服务商应用APPID与商家应用APPID，二选一必填。
                 * 2、可填写与商家主体一致且已认证的应用APPID，需是已认证的APP。
                 * 3、审核通过后，系统将发起特约商家商户号与该AppID的绑定（即配置为sub_appid），服务商随后可在发起支付时选择传入该appid，以完成支付，并获取sub_openid用于数据统计，营销等业务场景。
                 */
                private String app_sub_app_id;

                /**
                 * App截图
                 * 1、请提供APP首页截图、尾页截图、应用内截图、支付页截图各1张。
                 * 2、请填写通过图片上传API预先上传图片生成好的MediaID。
                 */
                private List<String> app_pics;

            }

            @Data
            public static class WebInfo {

                /**
                 * 互联网网站域名
                 * 1、如为PC端商城、智能终端等场景，可上传官网链接。
                 * 2、网站域名需ICP备案，若备案主体与申请主体不同，请上传加盖公章的网站授权函。
                 */
                private String domain;

                /**
                 * 网站授权函
                 * 1、若备案主体与申请主体不同，请务必上传加盖公章的网站<a href="https://wx.gtimg.com/mch/img/icp.doc">授权函</a>。
                 * 2、请填写通过图片上传API预先上传图片生成好的MediaID。
                 */
                private String web_authorisation;

                /**
                 * 互联网网站对应的商家AppID
                 * 1、可填写已认证的公众号、小程序、应用的APPID，其中公众号APPID需是已认证的服务 号、政府或媒体类型的订阅号；
                 * 2、完成进件后，系统发起特约商户号与该AppID的绑定（即配置为sub_appid，可在发起支付时传入）
                 * （1）若APPID主体与商家主体一致，则直接完成绑定；
                 * （2）若APPID主体与商家主体不一致，则商户签约时显示《联合营运承诺函》，并且 AppID的管理员需登录公众平台确认绑定意愿；（ 暂不支持绑定异主体的应用APPID）。
                 */
                private String web_appid;

            }

            @Data
            public static class WeworkInfo {

                /**
                 * 商家企业微信CorpID
                 * 1、可填写与商家主体一致且已认证的企业微信CorpID。
                 * 2、审核通过后，系统将为商家开通企业微信专区的自有交易权限，并完成商家商户号与该APPID的绑定，商家可自行发起交易。
                 */
                private String sub_corp_id;

                /**
                 * 企业微信页面截图
                 * 1、最多可上传5张照片
                 * 2、请填写通过图片上传API接口预先上传图片生成好的MediaID
                 */
                private List<String> wework_pics;

            }

        }

    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SettlementInfo {

        /**
         * 入驻结算规则ID
         * 请选择结算规则ID，详细参见<a href="https://kf.qq.com/faq/220228IJb2UV220228uEjU3Q.html">费率结算规则对照表</a>
         */
        private String settlement_id;

        /**
         * 所属行业
         * 填写指定行业名称，详细参见<a href="https://kf.qq.com/faq/220228IJb2UV220228uEjU3Q.html">费率结算规则对照表</a>
         */
        private String qualification_type;

        /**
         * 特殊资质图片
         * 1、根据所属行业的特殊资质要求提供，详情查看费率结算规则对照表
         * 2、最多可上传5张照片，请填写通过图片上传API接口预先上传图片生成好的MediaID
         */
        private List<String> qualifications;

        /**
         * 优惠费率活动ID
         * 选择指定活动ID，如果商户有意向报名优惠费率活动，该字段必填。详细参见优惠费率活动对照表。
         * 20191030111cff5b5e
         * <a href="https://pay.weixin.qq.com/wiki/doc/apiv3_partner/terms_definition/chapter1_1_3.shtml#part-10">优惠费率活动对照表</a>
         */
        private String activities_id;

        /**
         * 优惠费率活动值
         * 根据优惠费率活动规则，由服务商自定义填写，支持两个小数点，需在优惠费率活动ID指定费率范围内
         */
        private String activities_rate;

        /**
         * 优惠费率活动补充材料
         * 1、根据所选优惠费率活动，提供相关材料，详细参见优惠费率活动对照表
         * 2、最多可上传5张照片，请填写通过图片上传API接口预先上传图片生成好的MediaID
         */
        private List<String> activities_additions;

    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BankAccountInfo {

        /**
         * 1、若主体为企业/政府机关/事业单位/社会组织，可填写：对公银行账户。
         * 2、若主体为个体户，可选择填写：对公银行账户或经营者个人银行卡。
         * BANK_ACCOUNT_TYPE_CORPORATE：对公银行账户
         * BANK_ACCOUNT_TYPE_PERSONAL：经营者个人银行卡
         */
        private String bank_account_type;

        /**
         * 开户名称
         * 1、选择“经营者个人银行卡”时，开户名称必须与“经营者证件姓名”一致，
         * 2、选择“对公银行账户”时，开户名称必须与营业执照上的“商户名称”一致
         */
        private String account_name;

        /**
         * 开户银行
         * 1、17家直连银行，请根据开户银行对照表直接填写银行名 ;
         * 2、非17家直连银行，该参数请填写为“其他银行”。
         * 工商银行 交通银行 招商银行 民生银行 中信银行 浦发银行
         * 兴业银行 光大银行 广发银行 平安银行 北京银行 华夏银行
         * 农业银行 建设银行 邮政储蓄 中国银行 宁波银行
         * 其他银行
         */
        private String account_bank;

        /**
         * 开户银行省市编码
         * 至少精确到市，详细参见省市区编号对照表。
         * 仅当省市区编号对照表中无对应的省市区编号时，可向上取该银行对应市级编号或省级编号。
         * <a href="https://pay.weixin.qq.com/wiki/doc/apiv3/download/%E7%9C%81%E5%B8%82%E5%8C%BA%E7%BC%96%E5%8F%B7%E5%AF%B9%E7%85%A7%E8%A1%A8.xlsx">省市区编号对照表</a>
         */
        private String bank_address_code;

        /**
         * 开户银行联行号
         * 1、17家直连银行无需填写，如为其他银行，则开户银行全称（含支行）和开户银行联行号二选一。
         * 2、详细参见开户银行全称（含支行）对照表。
         */
        private String bank_branch_id;

        /**
         * 开户银行全称（含支行）
         * 1、17家直连银行无需填写，如为其他银行，则开户银行全称（含支行）和 开户银行联行号二选一。
         * 2、需填写银行全称，如"深圳农村商业银行XXX支行"，详细参见开户银行全称（含支行）对照表。
         * <a href="https://pay.weixin.qq.com/wiki/doc/apiv3/download/%E3%80%8A%E5%BC%80%E6%88%B7%E9%93%B6%E8%A1%8C%E5%85%A8%E7%A7%B0%EF%BC%88%E5%90%AB%E6%94%AF%E8%A1%8C%EF%BC%89%E5%AF%B9%E7%85%A7%E8%A1%A8%E3%80%8B-2020.06.16.xlsx">开户银行全称（含支行）对照表</a>
         */
        private String bank_name;

        /**
         * 银行账号
         */
        private String account_number;

        /**
         * 银行账户证明材料
         * 1. 当主体类型是“政府机关/事业单位”时或所属行业为“党费”时，支持在有合法资金管理关系的情况下结算账户设置为非同名。
         * 2. 若结算账户设置为非同名，则需填写非同名证明材料，若结算账户为同名，则无需填写。
         */
        private AccountCertInfo account_cert_info;

        @Data
        public static class AccountCertInfo {

            /**
             * 结算证明函
             * 1. 请参照<a href="https://kf.qq.com/faq/220127YjURBN220127fuA7bE.html">示例图</a>打印结算证明函。
             * 2、可上传1张图片，请填写通过图片上传API预先上传图片生成好的MediaID。
             */
            private String settlement_cert_pic;

            /**
             * 关系证明函
             * 1. 请参照<a href="https://kf.qq.com/faq/220127YjURBN220127fuA7bE.html">示例图</a>打印关系证明函。
             * 2、可上传1张图片，请填写通过图片上传API预先上传图片生成好的MediaID。
             */
            private String relation_cert_pic;

            /**
             * 其他补充证明
             * 1. 请提供非同名结算的法律法规、政策通知、政府或上级部门公文等证明文件，以作上述材料的补充证明。
             * 2、可上传1-3张图片，请填写通过图片上传API预先上传图片生成好的MediaID。
             */
            private List<String> other_cert_pics;

        }

    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdditionInfo {

        /**
         * 法人开户承诺函
         * 1、请上传法定代表人或负责人亲笔签署的开户承诺函扫描件。亲笔签名承诺函内容清晰可见，不得有涂污，破损，字迹不清晰现象。
         * 2、请填写通过图片上传API预先上传图片生成好的MediaID。
         * <a href="https://kf.qq.com/faq/191018yUFjEj191018Vfmaei.html">开户承诺函扫描件</a>
         */
        private String legal_person_commitment;

        /**
         * 法人开户意愿视频
         * 1、建议法人按如下话术录制“法人开户意愿视频”：
         * 我是#公司全称#的法定代表人（或负责人），特此证明本公司申请的商户号为我司真实意愿开立且用于XX业务（或XX服务）。我司现有业务符合法律法规及腾讯的相关规定。
         * 2、支持上传5M内的视频，格式可为avi、wmv、mpeg、mp4、mov、mkv、flv、f4v、m4v、rmvb；
         * 3、请填写通过视频上传API预先上传视频生成好的MediaID 。
         */
        private String legal_person_video;

        /**
         * 补充材料
         * 1、最多可上传5张照片
         * 2、请填写通过图片上传API预先上传图片生成好的MediaID。
         */
        private List<String> business_addition_pics;

        /**
         * 补充说明
         */
        private String business_addition_msg;

    }

}
