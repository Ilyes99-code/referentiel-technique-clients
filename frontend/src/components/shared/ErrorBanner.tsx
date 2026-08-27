import React from "react";

interface ErrorBannerProps {
  message: string;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({ message }) => (
  <div className="login-error error-banner" role="alert">
    <span className="login-error-mark">!</span>
    <span>{message}</span>
  </div>
);
