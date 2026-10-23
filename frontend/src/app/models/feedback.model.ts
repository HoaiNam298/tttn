export type FeedbackKind = 'success' | 'info' | 'warning' | 'error';

export interface FeedbackAction {
  labelKey: string;
  route: string;
}

export interface FeedbackMessage {
  kind: FeedbackKind;
  messageKey: string;
  action?: FeedbackAction;
}
