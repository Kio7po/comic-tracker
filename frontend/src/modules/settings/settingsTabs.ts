import type { LucideIcon } from 'lucide-react';
import { Palette, UserRound } from 'lucide-react';

export interface SettingsTab {
  value: string;
  path: string;
  icon: LucideIcon;
  labelKey: string;
}

// Extend this list to add another settings tab - the desktop sidebar and the mobile list are
// both driven by it, nothing else needs touching.
export const SETTINGS_TABS: SettingsTab[] = [
  { value: 'profile', path: '/settings/profile', icon: UserRound, labelKey: 'settings.tabs.profile' },
  { value: 'appearance', path: '/settings/appearance', icon: Palette, labelKey: 'settings.tabs.appearance' },
];
