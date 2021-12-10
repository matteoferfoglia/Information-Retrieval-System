fprintf("Determination of forward pointers position in a skip list");
% Rule: for a postingList of P postings, use F = ceil(sqrt(P)) evenly
%       spaced (with space S = floor(P/F) ) forward pointers; last posting
%       is never a forward pointer.

for P = 0:21
    F = ceil(sqrt(P));
    S = floor(P/F);
    positions = 0 : P-1;
    isFwdPointer = mod(positions, F)==0;
    if(~isempty(isFwdPointer)),  isFwdPointer(length(isFwdPointer)) = 0; end   % last posting does not need to be a forward pointer
    forwardPositions =positions(isFwdPointer);
    fprintf("\n\nP=%d   F=%d   S=%d", P,F,S);
    fprintf("\nPositions= \t\t") ; for i=1:length(positions), fprintf("\t%d", positions(i)), end
    fprintf("\nisFwdPointer= \t"); for i=1:length(isFwdPointer), fprintf("\t%d", isFwdPointer(i)), end
    fprintf("\nFwdPositions= \t"); for i=1:length(forwardPositions), fprintf("\t%d", forwardPositions(i)), end
end
fprintf("\n");