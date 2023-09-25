import { ReactNode } from "react";

interface FaneMalTiltakProps {
  children: ReactNode;
  harInnhold: boolean;
  className?: string;
}

const FaneTiltaksinformasjon = ({ children, harInnhold, className }: FaneMalTiltakProps) => {
  return <div className={className}>{harInnhold ? children : null}</div>;
};

export default FaneTiltaksinformasjon;
