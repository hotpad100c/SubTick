package subtick;

import carpet.settings.Rule;

public class Settings
{
  @Rule(
          desc = "carpet.rule.subtickDefaultPhase.desc",
          options = {"worldBorder", "weather", "time", "blockTick", "fluidTick", "raid", "chunk", "blockEvent", "entity", "blockEntity", "entityManagement"},
          category = "subtick"
  )
  public static String subtickDefaultPhase = "blockTick";

  @Rule(
          desc = "carpet.rule.subtickTextFormat.desc",
          category = "subtick"
  )
  public static String subtickTextFormat = "ig";

  @Rule(
          desc = "carpet.rule.subtickNumberFormat.desc",
          category = "subtick"
  )
  public static String subtickNumberFormat = "iy";

  @Rule(
          desc = "carpet.rule.subtickPhaseFormat.desc",
          category = "subtick"
  )
  public static String subtickPhaseFormat = "it";

  @Rule(
          desc = "carpet.rule.subtickDimensionFormat.desc",
          category = "subtick"
  )
  public static String subtickDimensionFormat = "im";

  @Rule(
          desc = "carpet.rule.subtickErrorFormat.desc",
          category = "subtick"
  )
  public static String subtickErrorFormat = "ir";

  @Rule(
          desc = "carpet.rule.subtickDefaultRange.desc",
          category = "subtick"
  )
  public static int subtickDefaultRange = 32;
}