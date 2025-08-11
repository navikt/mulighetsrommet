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
        gap: "0.5rem",
        padding: "0px 0.5rem",
        margin: "5px 0px",
      }}
    >
      <GrCircleInformation
        style={{
          alignSelf: "center",
        }}
      />
      <p>{children}</p>
    </div>
  );
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
      <small style={{ marginLeft: "4px", textAlign: "right" }}>{children}</small>
    </div>
  );
}

export function PreviewContainer({ children }) {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
      }}
    >
      {children}
    </div>
  );
}

export function SidemenyDetaljerContainer({ children }) {
  return (
    <small
      style={{
        border: "1px dashed black",
        padding: "4px 20px",
        display: "flex",
        flexDirection: "column",
        gap: "0.5rem",
      }}
    >
      {children}
    </small>
  );
}

export function SidemenyDetaljerRad({ navn, children }) {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "row",
        justifyContent: "space-between",
        height: "auto",
      }}
    >
      <h4
        style={{
          margin: "0",
        }}
      >
        {navn}
      </h4>
      <div
        style={{
          margin: "0",
        }}
      >
        {children}
      </div>
    </div>
  );
}
