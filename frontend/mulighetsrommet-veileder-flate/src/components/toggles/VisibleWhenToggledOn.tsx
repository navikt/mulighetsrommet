import { FeatureToggle } from "@api-client";
import { ReactNode } from "react";
import { useFeatureToggle } from "../../api/feature-toggles";

interface Props {
  children: ReactNode;
  toggle: FeatureToggle;
}

export function VisibleWhenToggledOn({ children, toggle }: Props) {
  const { data: isEnabled } = useFeatureToggle(toggle);

  return isEnabled ? <>{children}</> : null;
}
