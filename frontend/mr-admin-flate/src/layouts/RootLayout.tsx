import { ReactNode } from "react";
import styles from "./RootLayout.module.scss";
import { ForsideTiltaksansvarlig } from "../ForsideTiltaksansvarlig";
import { ForsideFagansvarlig } from "../ForsideFagansvarlig";

interface RootLayoutProps {
  fagansvarlig?: boolean;
  children: ReactNode;
}

export function RootLayout({
  fagansvarlig = false,
  children,
}: RootLayoutProps) {
  return (
    <>
      {fagansvarlig ? <ForsideFagansvarlig /> : <ForsideTiltaksansvarlig />}
      <main className={styles.container}>{children}</main>
    </>
  );
}
