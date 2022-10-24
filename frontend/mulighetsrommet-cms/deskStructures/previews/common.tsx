import React from "react";
import { GrCircleInformation } from "react-icons/gr";

export function Infoboks({ children }) {
  if (!children) return null;

  return (
    <div
      style={{
        backgroundColor: "#ebfcff",
        border: "1px solid black",
        display: "flex",
        alignItems: "baseline",
        gap: "10px",
        padding: "0px 10px",
        margin: "5px 0px",
      }}
    >
      <GrCircleInformation />
      <p>{children}</p>
    </div>
  );
}

export function MarginBottom({ children }) {
  return <div style={{ marginBottom: "4rem" }}>{children}</div>;
}

export function Firkant({ farge }) {
  return (
    <div
      style={{
        display: "inline-block",
        background: farge,
        height: "12px",
        width: "12px",
      }}
    />
  );
}

export function Legend({ farge, children }) {
  return (
    <div style={{ display: "flex", alignItems: "center" }}>
      <Firkant farge={farge} />
      <small style={{ marginLeft: "4px", textAlign: "right" }}>
        {children}
      </small>
    </div>
  );
}
