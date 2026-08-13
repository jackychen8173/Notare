import { Avatar, AvatarFallback } from "@/components/ui/avatar";

function initials(name: string) {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

interface StudentAvatarProps {
  name: string;
  size?: "default" | "sm" | "lg";
}

export function StudentAvatar({ name, size = "default" }: StudentAvatarProps) {
  return (
    <Avatar size={size}>
      <AvatarFallback>{initials(name)}</AvatarFallback>
    </Avatar>
  );
}
