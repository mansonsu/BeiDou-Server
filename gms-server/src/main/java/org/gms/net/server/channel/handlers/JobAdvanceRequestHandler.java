package org.gms.net.server.channel.handlers;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.client.Job;
import org.gms.client.QuestStatus;
import org.gms.client.Skill;
import org.gms.client.SkillFactory;
import org.gms.client.inventory.InventoryType;
import org.gms.client.inventory.manipulator.InventoryManipulator;
import org.gms.config.GameConfig;
import org.gms.constants.inventory.ItemConstants;
import org.gms.net.AbstractPacketHandler;
import org.gms.net.packet.InPacket;
import org.gms.server.quest.Quest;
import org.gms.util.PacketCreator;

import java.util.Map;
import java.util.Set;

public class JobAdvanceRequestHandler extends AbstractPacketHandler {
    private static final Map<Integer, Set<Integer>> DIRECT_ADVANCEMENTS = Map.ofEntries(
            Map.entry(0, Set.of(100, 200, 300, 400, 500)),
            Map.entry(100, Set.of(110, 120, 130)),
            Map.entry(200, Set.of(210, 220, 230)),
            Map.entry(300, Set.of(310, 320)),
            Map.entry(400, Set.of(410, 420)),
            Map.entry(500, Set.of(510, 520))
    );
    private static final int SECOND_JOB_PROOF = 4031012;
    private static final int THIRD_JOB_WISDOM_NECKLACE = 4031058;
    private static final int FOURTH_JOB_SKILL_BOOK = 2280003;
    private static final Map<Integer, Integer> FOURTH_JOB_QUESTS = Map.of(
            1, 6904,
            2, 6914,
            3, 6924,
            4, 6934,
            5, 6944
    );

    @Override
    public void handlePacket(InPacket p, Client c) {
        Character chr = c.getPlayer();
        if (chr == null) {
            return;
        }

        int targetJobId = p.readInt();
        Job targetJob = findJob(targetJobId);
        if (targetJob == null) {
            reject(c, chr, "未知的轉職職業。");
            return;
        }

        AdvancementResult result = validate(chr, targetJobId);
        if (!result.success()) {
            reject(c, chr, result.message());
            return;
        }

        chr.changeJob(targetJob);
        applyScriptSideEffects(c, chr, targetJobId, result.tier());
        chr.dropMessage(5, "【轉職系統】已轉職為 " + targetJob.getName());
    }

    private static AdvancementResult validate(Character chr, int targetJobId) {
        int currentJobId = chr.getJob().getId();
        int level = chr.getLevel();

        if (currentJobId == targetJobId) {
            return AdvancementResult.fail("你目前已經是這個職業。");
        }

        if (!isExplorerJob(currentJobId) || !isExplorerJob(targetJobId)) {
            return AdvancementResult.fail("直接轉職封包目前只開放冒險者職業。");
        }

        Set<Integer> directTargets = DIRECT_ADVANCEMENTS.get(currentJobId);
        if (directTargets != null) {
            if (!directTargets.contains(targetJobId)) {
                return AdvancementResult.fail("目前職業不能轉職為目標職業。");
            }

            if (currentJobId % 1000 == 0) {
                int requiredLevel = targetJobId == 200 ? 8 : 10;
                if (level < requiredLevel) {
                    return AdvancementResult.fail("等級不足，需要 Lv." + requiredLevel + "。");
                }
                AdvancementResult statResult = validateFirstJobStat(chr, targetJobId);
                if (!statResult.success()) {
                    return statResult;
                }
                return AdvancementResult.ok(1);
            }

            if (currentJobId % 100 == 0) {
                if (level < 30) {
                    return AdvancementResult.fail("等級不足，需要 Lv.30。");
                }
                if (!hasItem(chr, SECOND_JOB_PROOF, 1)) {
                    return AdvancementResult.fail("缺少二轉證明 #t" + SECOND_JOB_PROOF + "#。");
                }
                return AdvancementResult.ok(2);
            }
        }

        if (currentJobId % 10 == 0) {
            if (targetJobId != currentJobId + 1) {
                return AdvancementResult.fail("目前職業不能轉職為目標職業。");
            }
            if (level < 70) {
                return AdvancementResult.fail("等級不足，需要 Lv.70。");
            }
            if (!hasItem(chr, THIRD_JOB_WISDOM_NECKLACE, 1)) {
                return AdvancementResult.fail("缺少三轉智慧項鍊 #t" + THIRD_JOB_WISDOM_NECKLACE + "#。");
            }
            if (chr.getRemainingSp() > (level - 70) * 3) {
                return AdvancementResult.fail("請先使用 70 級以前取得的 SP。");
            }
            return AdvancementResult.ok(3);
        }

        if (currentJobId % 10 == 1) {
            if (currentJobId / 1000 >= 1 && currentJobId / 1000 < 2) {
                return AdvancementResult.fail("騎士團依原快速轉職腳本只開放到三轉。");
            }
            if (targetJobId != currentJobId + 1) {
                return AdvancementResult.fail("目前職業不能轉職為目標職業。");
            }
            if (level < 120) {
                return AdvancementResult.fail("等級不足，需要 Lv.120。");
            }
            int branch = currentJobId / 100;
            Integer questId = FOURTH_JOB_QUESTS.get(branch);
            if (questId == null) {
                return AdvancementResult.fail("目前職業沒有對應的四轉任務。");
            }
            if (!isQuestCompleted(chr, questId)) {
                return AdvancementResult.fail("尚未完成四轉任務 " + questId + "。");
            }
            if (!chr.canHold(FOURTH_JOB_SKILL_BOOK, 1)) {
                return AdvancementResult.fail("消耗欄空間不足，無法取得四轉技能書。");
            }
            return AdvancementResult.ok(4);
        }

        return AdvancementResult.fail("目前職業已無可用轉職。");
    }

    private static AdvancementResult validateFirstJobStat(Character chr, int targetJobId) {
        if (GameConfig.getServerBoolean("use_auto_assign_starters_ap")) {
            return AdvancementResult.ok(1);
        }

        return switch (targetJobId) {
            case 100 -> chr.getStr() >= 35 ? AdvancementResult.ok(1) : AdvancementResult.fail("力量不足，需要 35。");
            case 200 -> chr.getInt() >= 20 ? AdvancementResult.ok(1) : AdvancementResult.fail("智力不足，需要 20。");
            case 300, 400 -> chr.getDex() >= 25 ? AdvancementResult.ok(1) : AdvancementResult.fail("敏捷不足，需要 25。");
            case 500 -> chr.getDex() >= 20 ? AdvancementResult.ok(1) : AdvancementResult.fail("敏捷不足，需要 20。");
            default -> AdvancementResult.ok(1);
        };
    }

    private static boolean isExplorerJob(int jobId) {
        return jobId >= 0 && jobId <= 522;
    }

    private static void applyScriptSideEffects(Client c, Character chr, int targetJobId, int tier) {
        if (tier == 1) {
            chr.resetStats();
        } else if (tier == 2) {
            removeItem(c, SECOND_JOB_PROOF, 1);
        } else if (tier == 3) {
            removeItem(c, THIRD_JOB_WISDOM_NECKLACE, 1);
            chr.removePartyQuestItem("JBQ");
        } else if (tier == 4) {
            teachFourthJobStarterSkills(chr, targetJobId);
            InventoryManipulator.addById(c, FOURTH_JOB_SKILL_BOOK, (short) 1, "", -1);
        }
    }

    private static boolean hasItem(Character chr, int itemId, int quantity) {
        return chr.getItemQuantity(itemId, false) >= quantity;
    }

    private static void removeItem(Client c, int itemId, int quantity) {
        InventoryManipulator.removeById(c, ItemConstants.getInventoryType(itemId), itemId, quantity, true, false);
    }

    private static boolean isQuestCompleted(Character chr, int questId) {
        return chr.getQuest(Quest.getInstance(questId)).getStatus() == QuestStatus.Status.COMPLETED;
    }

    private static void teachFourthJobStarterSkills(Character chr, int jobId) {
        int[] skills = switch (jobId) {
            case 112 -> new int[]{1121001, 1120004, 1121008};
            case 122 -> new int[]{1221001, 1220005, 1221009};
            case 132 -> new int[]{1321001, 1320005, 1321007};
            case 212 -> new int[]{2121001, 2121002, 2121006};
            case 222 -> new int[]{2221001, 2221002, 2221006};
            case 232 -> new int[]{2321001, 2321002, 2321005};
            case 312 -> new int[]{3121002, 3121006};
            case 322 -> new int[]{3221002, 3221005};
            case 412 -> new int[]{4121003, 4120005, 4121006};
            case 422 -> new int[]{4221001, 4220002, 4221007};
            case 512 -> new int[]{5121001, 5121002, 5121009};
            case 522 -> new int[]{5220001, 5220002, 5221004};
            default -> new int[0];
        };

        for (int skillId : skills) {
            Skill skill = SkillFactory.getSkill(skillId);
            if (skill != null && chr.getSkillLevel(skill) <= 0) {
                chr.changeSkillLevel(skill, (byte) 0, 10, -1);
            }
        }
    }

    private static Job findJob(int jobId) {
        for (Job job : Job.values()) {
            if (job.getId() == jobId) {
                return job;
            }
        }
        return null;
    }

    private static void reject(Client c, Character chr, String message) {
        chr.dropMessage(5, "【轉職系統】" + message);
        c.sendPacket(PacketCreator.enableActions());
    }

    private record AdvancementResult(boolean success, String message, int tier) {
        static AdvancementResult ok(int tier) {
            return new AdvancementResult(true, "", tier);
        }

        static AdvancementResult fail(String message) {
            return new AdvancementResult(false, message, 0);
        }
    }
}
