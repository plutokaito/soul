/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.soul.admin.service.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.soul.admin.mapper.DataPermissionMapper;
import org.dromara.soul.admin.mapper.RuleMapper;
import org.dromara.soul.admin.mapper.SelectorMapper;
import org.dromara.soul.admin.model.dto.DataPermissionDTO;
import org.dromara.soul.admin.model.entity.DataPermissionDO;
import org.dromara.soul.admin.model.entity.RuleDO;
import org.dromara.soul.admin.model.entity.SelectorDO;
import org.dromara.soul.admin.model.page.CommonPager;
import org.dromara.soul.admin.model.page.PageResultUtils;
import org.dromara.soul.admin.model.query.RuleQuery;
import org.dromara.soul.admin.model.query.SelectorQuery;
import org.dromara.soul.admin.model.vo.DataPermissionPageVO;
import org.dromara.soul.admin.service.DataPermissionService;
import org.dromara.soul.common.enums.AdminDataPermissionTypeEnum;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * data permission vo.
 *
 * @author kaitoshy(plutokaito)
 */
@Service("dataPermissionService")
public class DataPermissionServiceImpl implements DataPermissionService {

    private final DataPermissionMapper dataPermissionMapper;

    private final RuleMapper ruleMapper;

    private final SelectorMapper selectorMapper;

    public DataPermissionServiceImpl(final DataPermissionMapper dataPermissionMapper,
                                     final RuleMapper ruleMapper,
                                     final SelectorMapper selectorMapper) {
        this.dataPermissionMapper = dataPermissionMapper;
        this.ruleMapper = ruleMapper;
        this.selectorMapper = selectorMapper;
    }

    /**
     * Get all data permissions by user id.
     *
     * @param userId user id
     * @return list of {@linkplain DataPermissionDO}
     */
    @Override
    public List<DataPermissionDO> getUserDataPermissionList(final String userId) {
        if (StringUtils.isBlank(userId)) {
            return null;
        }
        return dataPermissionMapper.listByUserId(userId);
    }


    /**
     * Create data permissions.
     *
     * @param dataPermissionDTO {@linkplain DataPermissionDTO}
     * @return int
     */
    @Override
    public int createSelector(final DataPermissionDTO dataPermissionDTO) {
        List<DataPermissionDO> allRuleDo = ruleMapper.findBySelectorId(dataPermissionDTO.getDataId())
                .stream()
                .filter(Objects::nonNull)
                .map(ruleDO -> DataPermissionDO.buildPermissionDO(ruleDO, dataPermissionDTO.getUserId()))
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(allRuleDo)) {
            allRuleDo.add(DataPermissionDO.buildPermissionDO(dataPermissionDTO));

            allRuleDo.forEach(dataPermissionMapper::insertSelective);

            return allRuleDo.size();
        }

        return 0;
    }


    /**
     * deleteSelector data permission.
     *
     * @param dataPermissionDTO {@linkplain DataPermissionDTO}
     * @return int  effect rows
     */
    @Override
    public int deleteSelector(final DataPermissionDTO dataPermissionDTO) {
        List<String> allRuleIds = ruleMapper.findBySelectorId(dataPermissionDTO.getDataId())
                .stream()
                .filter(Objects::nonNull)
                .map(RuleDO::getId)
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(allRuleIds)) {
            allRuleIds.add(dataPermissionDTO.getDataId());
            return dataPermissionMapper.deleteByDataIdsAndUserId(allRuleIds, dataPermissionDTO.getUserId());
        }

        return 0;
    }

    /**
     * list of selectors.
     *
     * @param selectorQuery {@linkplain SelectorQuery}
     * @param userId        user id
     * @return {@linkplain CommonPager}
     */
    @Override
    public CommonPager<DataPermissionPageVO> listSelectorsByPage(final SelectorQuery selectorQuery, final String userId) {
        int totalCount = selectorMapper.countByQuery(selectorQuery);

        Supplier<Stream<SelectorDO>> selectorDOStreamSupplier = () -> selectorMapper.selectByQuery(selectorQuery).stream();
        List<String> selectorIds = selectorDOStreamSupplier.get().map(SelectorDO::getId).collect(Collectors.toList());

        List<String> hasDataPermissionSelectorIds = dataPermissionMapper.selectDataIdsByDataIdsAndUserId(selectorIds,
                userId, AdminDataPermissionTypeEnum.SELECTOR.ordinal());

        List<DataPermissionPageVO> selectorList = selectorDOStreamSupplier.get().map(selectorDO -> {
            boolean isChecked = hasDataPermissionSelectorIds.contains(selectorDO.getId());
            return DataPermissionPageVO.buildPageVOBySelector(selectorDO, isChecked);
        }).collect(Collectors.toList());

        return PageResultUtils.result(selectorQuery.getPageParameter(), () -> totalCount, () -> selectorList);
    }

    /**
     * list of rules.
     *
     * @param ruleQuery {@linkplain RuleQuery}
     * @param userId    user id
     * @return {@linkplain CommonPager}
     */
    @Override
    public CommonPager<DataPermissionPageVO> listRulesByPage(final RuleQuery ruleQuery, final String userId) {
        int totalCount = ruleMapper.countByQuery(ruleQuery);

        Supplier<Stream<RuleDO>> ruleDOStreamSupplier = () -> ruleMapper.selectByQuery(ruleQuery).stream();
        List<String> ruleIds = ruleDOStreamSupplier.get().map(RuleDO::getId).collect(Collectors.toList());

        List<String> hasDataPermissionRuleIds = dataPermissionMapper.selectDataIdsByDataIdsAndUserId(ruleIds,
                userId, AdminDataPermissionTypeEnum.RULE.ordinal());

        List<DataPermissionPageVO> selectorList = ruleDOStreamSupplier.get().map(ruleDO -> {
            boolean isChecked = hasDataPermissionRuleIds.contains(ruleDO.getId());
            return DataPermissionPageVO.buildPageVOByRule(ruleDO, isChecked);
        }).collect(Collectors.toList());

        return PageResultUtils.result(ruleQuery.getPageParameter(), () -> totalCount, () -> selectorList);
    }

}
