import React from "react";
import { Link, useLocation } from "react-router-dom";

interface Props {
  // side: string;
}

export function Breadcrumbs() {
  const location = useLocation();
  //const [side] = useAtom(sideAtom(false));
  // console.log("location", location);
  let currentLink = "";

  const crumbs = location.pathname
    .split("/")
    .filter((crumb) => crumb !== "")
    .map((crumb) => {
      currentLink += `/${crumb}`;

      return (
        <div className="crumb" key={crumb}>
          <Link to={currentLink}>{crumb}</Link>
        </div>
      );
    });

  return <div>{crumbs}</div>;
}
