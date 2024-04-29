import { Toggles } from "mulighetsrommet-api-client";
import { useFeatureToggle } from "@/api/features/useFeatureToggle";

interface Props {
  value: string;
}

export function ShowOpphavValue({ value }: Props) {
  const { data: isDebugEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_ENABLE_DEBUGGER,
  );

  if (isDebugEnabled) {
    return <small>Opphav: {value}</small>;
  }

  return null;
}
