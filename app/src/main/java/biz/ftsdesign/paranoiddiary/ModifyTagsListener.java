package biz.ftsdesign.paranoiddiary;

import java.util.List;

interface ModifyTagsListener {
    void modifyTags(List<Long> tagsToSetIds, List<Long> tagsToUnsetIds);
}
